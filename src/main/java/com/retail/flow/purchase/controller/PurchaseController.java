package com.retail.flow.purchase.controller;

import com.retail.flow.purchase.dto.PurchaseRequestDto;
import com.retail.flow.purchase.dto.PurchaseResponseDto;
import com.retail.flow.purchase.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<PurchaseResponseDto> createPurchase(@Valid @RequestBody PurchaseRequestDto requestDto) {
        PurchaseResponseDto response = purchaseService.createPurchase(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseResponseDto>> getAllPurchases() {
        List<PurchaseResponseDto> purchases = purchaseService.getAllPurchases();
        return ResponseEntity.ok(purchases);
    }
}