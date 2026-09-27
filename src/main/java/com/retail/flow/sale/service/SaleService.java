package com.retail.flow.sale.service;

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
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public SaleResponseDto createSale(SaleRequestDto requestDto) {
        Customer customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + requestDto.getCustomerId()));

        Sale sale = Sale.builder()
                .saleNumber(requestDto.getSaleNumber())
                .customer(customer)
                .saleDate(requestDto.getSaleDate())
                .saleItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var itemDto : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + itemDto.getVariantId()));

            if (variant.getStock() < itemDto.getQuantity()) {
                throw new RuntimeException("Insufficient stock for SKU: " + variant.getSku());
            }

            int previousStock = variant.getStock();
            int newStock = previousStock - itemDto.getQuantity();

            // Update variant stock
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

            // Record Inventory Transaction
            InventoryTransaction transaction = InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.SALE)
                    .quantity(itemDto.getQuantity())
                    .referenceType("SALE")
                    .referenceId(null)
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build();

            inventoryTransactionRepository.save(transaction);
        }

        sale.setTotalAmount(totalAmount);
        Sale savedSale = saleRepository.save(sale);

        return mapToResponseDto(savedSale);
    }

    public List<SaleResponseDto> getAllSales() {
        return saleRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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