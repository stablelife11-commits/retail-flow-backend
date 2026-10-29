package com.retail.flow.sale.controller;

import com.retail.flow.sale.dto.SaleRequestDto;
import com.retail.flow.sale.dto.SaleResponseDto;
import com.retail.flow.sale.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    public ResponseEntity<SaleResponseDto> createSale(
            @Valid @RequestBody SaleRequestDto requestDto,
            Authentication authentication) {
        SaleResponseDto response = saleService.createSale(requestDto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SaleResponseDto>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        List<SaleResponseDto> sales = saleService.getAllSales(authentication.getName(), page, size);
        return ResponseEntity.ok(sales);
    }
}