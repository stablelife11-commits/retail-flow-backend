package com.retail.flow.returns.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
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
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final SaleReturnRepository returnRepository;
    private final ProductVariantRepository variantRepository;
    private final SaleRepository saleRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReturnResponseDto processReturn(ReturnRequestDto requestDto, String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Sale sale = saleRepository.findById(requestDto.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + requestDto.getSaleId()));

        if (caller.getRole() != User.Role.ADMIN && !sale.getSellerId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to process returns for another seller's sale.");
        }

        Map<Long, Integer> soldQuantityByVariant = sale.getSaleItems().stream()
                .collect(Collectors.groupingBy(
                        si -> si.getProductVariant().getId(),
                        Collectors.summingInt(SaleItem::getQuantity)
                ));

        List<SaleReturn> previousReturns = returnRepository.findBySaleId(sale.getId());
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
                .sellerId(sale.getSellerId())
                .saleId(sale.getId())
                .returnDate(LocalDate.now())
                .reason(requestDto.getReason())
                .items(new ArrayList<>())
                .build();

        List<InventoryTransaction> pendingAuditLogs = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : requestedReturnByVariant.entrySet()) {
            Long variantId = entry.getKey();
            int requestedQty = entry.getValue();

            Integer originallySoldQty = soldQuantityByVariant.get(variantId);
            if (originallySoldQty == null) {
                throw new BusinessValidationException(
                        "Variant ID " + variantId + " was not part of Sale ID " + sale.getId());
            }

            int previouslyReturnedQty = alreadyReturnedByVariant.getOrDefault(variantId, 0);
            int remainingReturnableQty = originallySoldQty - previouslyReturnedQty;

            if (requestedQty > remainingReturnableQty) {
                throw new BusinessValidationException(
                        "Return quantity (" + requestedQty + ") exceeds remaining returnable quantity ("
                                + remainingReturnableQty + ") for Variant ID " + variantId
                                + " (Sold: " + originallySoldQty + ", Already Returned: " + previouslyReturnedQty + ")");
            }

            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

            int previousStock = variant.getStock();
            int newStock = previousStock + requestedQty;

            variant.setStock(newStock);
            variantRepository.save(variant);

            ReturnItem returnItem = ReturnItem.builder()
                    .saleReturn(saleReturn)
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .quantity(requestedQty)
                    .build();

            saleReturn.getItems().add(returnItem);

            pendingAuditLogs.add(InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.RETURN)
                    .quantity(requestedQty)
                    .referenceType("SALE_RETURN")
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build());
        }

        SaleReturn savedReturn = returnRepository.save(saleReturn);

        for (InventoryTransaction tx : pendingAuditLogs) {
            tx.setReferenceId(savedReturn.getId());
        }
        inventoryTransactionRepository.saveAll(pendingAuditLogs);

        List<ReturnResponseDto.ReturnItemResponseDto> itemResponseDtos = savedReturn.getItems().stream()
                .map(ri -> ReturnResponseDto.ReturnItemResponseDto.builder()
                        .id(ri.getId())
                        .variantId(ri.getVariantId())
                        .sku(ri.getSku())
                        .quantity(ri.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return ReturnResponseDto.builder()
                .id(savedReturn.getId())
                .saleId(savedReturn.getSaleId())
                .returnDate(savedReturn.getReturnDate())
                .reason(savedReturn.getReason())
                .items(itemResponseDtos)
                .build();
    }
}