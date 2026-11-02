package com.retail.flow.inventory.controller;

import com.retail.flow.common.security.CurrentUserService;
import com.retail.flow.inventory.dto.InventoryTransactionResponseDto;
import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
import com.retail.flow.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InventoryTransactionResponseDto>> getInventoryTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        User caller = currentUserService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by("id").descending()
        );

        Page<InventoryTransaction> txPage = (caller.getRole() == User.Role.ADMIN)
                ? inventoryTransactionRepository.findAll(pageable)
                : inventoryTransactionRepository.findBySellerId(caller.getId(), pageable);

        List<InventoryTransactionResponseDto> dtos = txPage.getContent().stream()
                .map(tx -> InventoryTransactionResponseDto.builder()
                        .id(tx.getId())
                        .variantId(tx.getProductVariant().getId())
                        .sku(tx.getProductVariant().getSku())
                        .productName(tx.getProductVariant().getProduct().getName())
                        .size(tx.getProductVariant().getSize())
                        .color(tx.getProductVariant().getColor())
                        .type(tx.getType().name())
                        .quantity(tx.getQuantity())
                        .referenceType(tx.getReferenceType())
                        .referenceId(tx.getReferenceId())
                        .previousStock(tx.getPreviousStock())
                        .newStock(tx.getNewStock())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}