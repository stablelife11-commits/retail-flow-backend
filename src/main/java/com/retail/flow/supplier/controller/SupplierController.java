package com.retail.flow.supplier.controller;

import com.retail.flow.supplier.dto.SupplierRequestDto;
import com.retail.flow.supplier.dto.SupplierResponseDto;
import com.retail.flow.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<SupplierResponseDto> createSupplier(@Valid @RequestBody SupplierRequestDto requestDto) {
        SupplierResponseDto response = supplierService.createSupplier(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponseDto>> getAllSuppliers() {
        List<SupplierResponseDto> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }
}