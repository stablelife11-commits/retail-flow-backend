package com.retail.flow.purchase.service;

import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.purchase.dto.PurchaseRequestDto;
import com.retail.flow.purchase.dto.PurchaseResponseDto;
import com.retail.flow.purchase.entity.Purchase;
import com.retail.flow.purchase.entity.PurchaseItem;
import com.retail.flow.purchase.repository.PurchaseRepository;
import com.retail.flow.supplier.entity.Supplier;
import com.retail.flow.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional
    public PurchaseResponseDto createPurchase(PurchaseRequestDto requestDto) {
        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + requestDto.getSupplierId()));

        Purchase purchase = Purchase.builder()
                .purchaseNumber(requestDto.getPurchaseNumber())
                .supplier(supplier)
                .purchaseDate(requestDto.getPurchaseDate())
                .purchaseItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var itemDto : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + itemDto.getVariantId()));

            int previousStock = variant.getStock();
            int newStock = previousStock + itemDto.getQuantity();

            // Update variant stock
            variant.setStock(newStock);
            productVariantRepository.save(variant);

            BigDecimal itemTotal = itemDto.getUnitCost().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .purchase(purchase)
                    .productVariant(variant)
                    .quantity(itemDto.getQuantity())
                    .unitCost(itemDto.getUnitCost())
                    .totalPrice(itemTotal)
                    .build();

            purchase.getPurchaseItems().add(purchaseItem);

            // Create Inventory Audit Trail Transaction
            InventoryTransaction transaction = InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.PURCHASE)
                    .quantity(itemDto.getQuantity())
                    .referenceType("PURCHASE")
                    .referenceId(null) // Will be updated or saved after purchase ID generation
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build();

            inventoryTransactionRepository.save(transaction);
        }

        purchase.setTotalAmount(totalAmount);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        return mapToResponseDto(savedPurchase);
    }

    public List<PurchaseResponseDto> getAllPurchases() {
        return purchaseRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private PurchaseResponseDto mapToResponseDto(Purchase purchase) {
        List<PurchaseResponseDto.PurchaseItemResponseDto> itemDtos = purchase.getPurchaseItems().stream()
                .map(item -> PurchaseResponseDto.PurchaseItemResponseDto.builder()
                        .id(item.getId())
                        .variantId(item.getProductVariant().getId())
                        .sku(item.getProductVariant().getSku())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return PurchaseResponseDto.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .supplierId(purchase.getSupplier().getId())
                .supplierName(purchase.getSupplier().getName())
                .purchaseDate(purchase.getPurchaseDate())
                .totalAmount(purchase.getTotalAmount())
                .items(itemDtos)
                .build();
    }
}