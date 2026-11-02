package com.retail.flow.returns.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.common.security.CurrentUserService;
import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.entity.Order.OrderStatus;
import com.retail.flow.order.entity.Order.PaymentStatus;
import com.retail.flow.order.entity.OrderItem;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.returns.dto.ReturnRequestDto;
import com.retail.flow.returns.dto.ReturnResponseDto;
import com.retail.flow.returns.entity.ReturnItem;
import com.retail.flow.returns.entity.SaleReturn;
import com.retail.flow.returns.repository.SaleReturnRepository;
import com.retail.flow.sale.entity.Sale;
import com.retail.flow.sale.entity.SaleItem;
import com.retail.flow.sale.repository.SaleRepository;
import com.retail.flow.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final SaleReturnRepository returnRepository;
    private final ProductVariantRepository variantRepository;
    private final SaleRepository saleRepository;
    private final OrderRepository orderRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public ReturnResponseDto processReturn(ReturnRequestDto requestDto, String callerEmail) {
        User caller = currentUserService.getCurrentUser(callerEmail);

        boolean hasSaleId = requestDto.getSaleId() != null;
        boolean hasOrderId = requestDto.getOrderId() != null;

        if (hasSaleId == hasOrderId) {
            throw new BusinessValidationException("Provide exactly one of 'saleId' (offline sale) or 'orderId' (delivered online order).");
        }

        Long authoritativeSellerId;
        Map<Long, Integer> purchasedQuantityByVariant = new HashMap<>();
        Map<Long, BigDecimal> unitPriceByVariant = new HashMap<>();
        List<SaleReturn> previousReturns;
        String auditReferenceType;
        Sale targetSale = null;
        Order targetOrder = null;

        if (hasSaleId) {
            targetSale = saleRepository.findById(requestDto.getSaleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + requestDto.getSaleId()));

            if (caller.getRole() != User.Role.ADMIN && !targetSale.getSellerId().equals(caller.getId())) {
                throw new AccessDeniedException("You are not authorized to process returns for another seller's sale.");
            }

            authoritativeSellerId = targetSale.getSellerId();
            for (SaleItem si : targetSale.getSaleItems()) {
                Long vId = si.getProductVariant().getId();
                purchasedQuantityByVariant.merge(vId, si.getQuantity(), Integer::sum);
                unitPriceByVariant.put(vId, si.getUnitPrice());
            }
            previousReturns = returnRepository.findBySaleId(targetSale.getId());
            auditReferenceType = "SALE_RETURN";
        } else {
            targetOrder = orderRepository.findById(requestDto.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + requestDto.getOrderId()));

            if (caller.getRole() != User.Role.ADMIN && !targetOrder.getSellerId().equals(caller.getId())) {
                throw new AccessDeniedException("You are not authorized to process returns for another seller's order.");
            }

            if (targetOrder.getStatus() != OrderStatus.DELIVERED) {
                throw new BusinessValidationException("Returns can only be processed for DELIVERED orders (Current status: "
                        + targetOrder.getStatus() + "). Use order cancellation/rejection for undelivered orders.");
            }

            authoritativeSellerId = targetOrder.getSellerId();
            for (OrderItem oi : targetOrder.getOrderItems()) {
                Long vId = oi.getProductVariant().getId();
                purchasedQuantityByVariant.merge(vId, oi.getQuantity(), Integer::sum);
                unitPriceByVariant.put(vId, oi.getPrice());
            }
            previousReturns = returnRepository.findByOrderId(targetOrder.getId());
            auditReferenceType = "ORDER_RETURN";
        }

        Map<Long, Integer> alreadyReturnedByVariant = previousReturns.stream()
                .flatMap(sr -> sr.getItems().stream())
                .collect(Collectors.groupingBy(
                        ReturnItem::getVariantId,
                        Collectors.summingInt(ReturnItem::getQuantity)
                ));

        Map<Long, Integer> requestedReturnByVariant = new LinkedHashMap<>();
        for (ReturnRequestDto.ReturnItemDto itemDto : requestDto.getItems()) {
            requestedReturnByVariant.merge(itemDto.getVariantId(), itemDto.getQuantity(), Integer::sum);
        }

        SaleReturn saleReturn = SaleReturn.builder()
                .sellerId(authoritativeSellerId)
                .saleId(requestDto.getSaleId())
                .orderId(requestDto.getOrderId())
                .returnDate(LocalDate.now())
                .reason(requestDto.getReason())
                .totalReturnAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalReturnAmount = BigDecimal.ZERO;
        List<InventoryTransaction> pendingAuditLogs = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : requestedReturnByVariant.entrySet()) {
            Long variantId = entry.getKey();
            int requestedQty = entry.getValue();

            Integer originallySoldQty = purchasedQuantityByVariant.get(variantId);
            if (originallySoldQty == null) {
                throw new BusinessValidationException(
                        "Variant ID " + variantId + " was not part of the referenced transaction.");
            }

            int previouslyReturnedQty = alreadyReturnedByVariant.getOrDefault(variantId, 0);
            int remainingReturnableQty = originallySoldQty - previouslyReturnedQty;

            if (requestedQty > remainingReturnableQty) {
                throw new BusinessValidationException(
                        "Return quantity (" + requestedQty + ") exceeds remaining returnable quantity ("
                                + remainingReturnableQty + ") for Variant ID " + variantId
                                + " (Purchased: " + originallySoldQty + ", Already Returned: " + previouslyReturnedQty + ")");
            }

            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

            int previousStock = variant.getStock();
            int newStock = previousStock + requestedQty;

            variant.setStock(newStock);
            variantRepository.save(variant);

            BigDecimal unitPrice = unitPriceByVariant.getOrDefault(variantId, variant.getSellingPrice());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            totalReturnAmount = totalReturnAmount.add(lineTotal);

            ReturnItem returnItem = ReturnItem.builder()
                    .saleReturn(saleReturn)
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .quantity(requestedQty)
                    .unitPrice(unitPrice)
                    .totalPrice(lineTotal)
                    .build();

            saleReturn.getItems().add(returnItem);

            pendingAuditLogs.add(InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.RETURN)
                    .quantity(requestedQty)
                    .referenceType(auditReferenceType)
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build());
        }

        saleReturn.setTotalReturnAmount(totalReturnAmount);
        SaleReturn savedReturn = returnRepository.save(saleReturn);

        // Fix F-04: Adjust receivable balance and paymentStatus on the parent Sale or Order
        if (targetSale != null) {
            BigDecimal currentReturned = targetSale.getReturnedAmount() != null ? targetSale.getReturnedAmount() : BigDecimal.ZERO;
            BigDecimal updatedReturned = currentReturned.add(totalReturnAmount);
            targetSale.setReturnedAmount(updatedReturned);

            BigDecimal netPayable = targetSale.getTotalAmount().subtract(updatedReturned).max(BigDecimal.ZERO);
            BigDecimal paid = targetSale.getPaidAmount() != null ? targetSale.getPaidAmount() : BigDecimal.ZERO;

            if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(netPayable) >= 0) {
                targetSale.setPaymentStatus(PaymentStatus.PAID);
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                targetSale.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            }
            saleRepository.save(targetSale);
        } else if (targetOrder != null) {
            BigDecimal currentReturned = targetOrder.getReturnedAmount() != null ? targetOrder.getReturnedAmount() : BigDecimal.ZERO;
            BigDecimal updatedReturned = currentReturned.add(totalReturnAmount);
            targetOrder.setReturnedAmount(updatedReturned);

            BigDecimal netPayable = targetOrder.getTotalAmount().subtract(updatedReturned).max(BigDecimal.ZERO);
            BigDecimal paid = targetOrder.getPaidAmount() != null ? targetOrder.getPaidAmount() : BigDecimal.ZERO;

            if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(netPayable) >= 0) {
                targetOrder.setPaymentStatus(PaymentStatus.PAID);
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                targetOrder.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            }
            orderRepository.save(targetOrder);
        }

        for (InventoryTransaction tx : pendingAuditLogs) {
            tx.setReferenceId(savedReturn.getId());
        }
        inventoryTransactionRepository.saveAll(pendingAuditLogs);

        return mapToResponseDto(savedReturn);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponseDto> getAllReturns(String callerEmail) {
        User caller = currentUserService.getCurrentUser(callerEmail);

        List<SaleReturn> returns = (caller.getRole() == User.Role.ADMIN)
                ? returnRepository.findAll()
                : returnRepository.findBySellerId(caller.getId());

        return returns.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private ReturnResponseDto mapToResponseDto(SaleReturn savedReturn) {
        List<ReturnResponseDto.ReturnItemResponseDto> itemResponseDtos = savedReturn.getItems().stream()
                .map(ri -> ReturnResponseDto.ReturnItemResponseDto.builder()
                        .id(ri.getId())
                        .variantId(ri.getVariantId())
                        .sku(ri.getSku())
                        .quantity(ri.getQuantity())
                        .unitPrice(ri.getUnitPrice())
                        .totalPrice(ri.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return ReturnResponseDto.builder()
                .id(savedReturn.getId())
                .saleId(savedReturn.getSaleId())
                .orderId(savedReturn.getOrderId())
                .returnDate(savedReturn.getReturnDate())
                .reason(savedReturn.getReason())
                .totalReturnAmount(savedReturn.getTotalReturnAmount())
                .items(itemResponseDtos)
                .build();
    }
}