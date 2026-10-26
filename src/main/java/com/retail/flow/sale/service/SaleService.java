package com.retail.flow.sale.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.sale.dto.SaleRequestDto;
import com.retail.flow.sale.dto.SaleResponseDto;
import com.retail.flow.sale.entity.Sale;
import com.retail.flow.sale.entity.SaleItem;
import com.retail.flow.sale.repository.SaleRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public SaleResponseDto createSale(SaleRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        Customer customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + requestDto.getCustomerId()));

        Sale sale = Sale.builder()
                .sellerId(caller.getId())
                .saleNumber(requestDto.getSaleNumber())
                .customer(customer)
                .saleDate(requestDto.getSaleDate())
                .saleItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InventoryTransaction> pendingTransactions = new ArrayList<>();

        for (var itemDto : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + itemDto.getVariantId()));

            if (caller.getRole() != User.Role.ADMIN && !variant.getProduct().getSellerId().equals(caller.getId())) {
                throw new AccessDeniedException("You cannot sell another seller's product variant (SKU: " + variant.getSku() + ").");
            }

            if (variant.getStock() < itemDto.getQuantity()) {
                throw new BusinessValidationException("Insufficient stock for SKU: " + variant.getSku());
            }

            int previousStock = variant.getStock();
            int newStock = previousStock - itemDto.getQuantity();

            variant.setStock(newStock);
            productVariantRepository.save(variant);

            BigDecimal unitPrice = variant.getSellingPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .productVariant(variant)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(itemTotal)
                    .build();

            sale.getSaleItems().add(saleItem);

            pendingTransactions.add(InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.SALE)
                    .quantity(itemDto.getQuantity())
                    .referenceType("SALE")
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build());
        }

        sale.setTotalAmount(totalAmount);
        Sale savedSale = saleRepository.save(sale);

        for (InventoryTransaction tx : pendingTransactions) {
            tx.setReferenceId(savedSale.getId());
        }
        inventoryTransactionRepository.saveAll(pendingTransactions);

        return mapToResponseDto(savedSale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponseDto> getAllSales(String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        List<Sale> sales = (caller.getRole() == User.Role.ADMIN)
                ? saleRepository.findAll()
                : saleRepository.findBySellerId(caller.getId());

        return sales.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private SaleResponseDto mapToResponseDto(Sale sale) {
        List<SaleResponseDto.SaleItemResponseDto> itemDtos = sale.getSaleItems().stream()
                .map(item -> SaleResponseDto.SaleItemResponseDto.builder()
                        .id(item.getId())
                        .variantId(item.getProductVariant().getId())
                        .sku(item.getProductVariant().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return SaleResponseDto.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .customerId(sale.getCustomer().getId())
                .customerName(sale.getCustomer().getName())
                .saleDate(sale.getSaleDate())
                .totalAmount(sale.getTotalAmount())
                .items(itemDtos)
                .build();
    }
}