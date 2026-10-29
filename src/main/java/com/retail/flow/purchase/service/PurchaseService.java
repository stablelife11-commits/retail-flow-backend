package com.retail.flow.purchase.service;

import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.common.security.CurrentUserService;
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
import com.retail.flow.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
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
    private final CurrentUserService currentUserService;

    @Transactional
    public PurchaseResponseDto createPurchase(PurchaseRequestDto requestDto, String callerEmail) {
        User caller = currentUserService.getCurrentUser(callerEmail);

        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + requestDto.getSupplierId()));

        if (caller.getRole() != User.Role.ADMIN && !supplier.getSellerId().equals(caller.getId())) {
            throw new AccessDeniedException("You cannot record a purchase using another seller's supplier.");
        }

        Long authoritativeSellerId = supplier.getSellerId();

        Purchase purchase = Purchase.builder()
                .sellerId(authoritativeSellerId)
                .purchaseNumber(requestDto.getPurchaseNumber().trim())
                .supplier(supplier)
                .purchaseDate(requestDto.getPurchaseDate())
                .purchaseItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InventoryTransaction> pendingTransactions = new ArrayList<>();

        for (var itemDto : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + itemDto.getVariantId()));

            if (!variant.getProduct().getSellerId().equals(authoritativeSellerId)) {
                throw new AccessDeniedException("Variant SKU " + variant.getSku() + " does not belong to your seller catalog.");
            }

            int previousStock = variant.getStock();
            int newStock = previousStock + itemDto.getQuantity();

            variant.setStock(newStock);
            variant.setPurchasePrice(itemDto.getUnitCost());
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

            pendingTransactions.add(InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.PURCHASE)
                    .quantity(itemDto.getQuantity())
                    .referenceType("PURCHASE")
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build());
        }

        purchase.setTotalAmount(totalAmount);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        for (InventoryTransaction tx : pendingTransactions) {
            tx.setReferenceId(savedPurchase.getId());
        }
        inventoryTransactionRepository.saveAll(pendingTransactions);

        return mapToResponseDto(savedPurchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponseDto> getAllPurchases(String callerEmail, int page, int size) {
        User caller = currentUserService.getCurrentUser(callerEmail);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending());

        Page<Purchase> purchases = (caller.getRole() == User.Role.ADMIN)
                ? purchaseRepository.findAll(pageable)
                : purchaseRepository.findBySellerId(caller.getId(), pageable);

        return purchases.stream()
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