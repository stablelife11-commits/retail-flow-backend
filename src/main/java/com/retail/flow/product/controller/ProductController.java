package com.retail.flow.product.controller;

import com.retail.flow.common.dto.PagedResponseDto;
import com.retail.flow.product.dto.ProductRequestDto;
import com.retail.flow.product.dto.ProductResponseDto;
import com.retail.flow.product.dto.StockUpdateRequestDto;
import com.retail.flow.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody ProductRequestDto requestDto,
            Authentication authentication) {
        ProductResponseDto response = productService.createProduct(requestDto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Backward-compatible endpoint for Android/Web clients expecting List<ProductResponseDto>
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long sellerId,
            Authentication authentication) {
        List<ProductResponseDto> products = productService.getAllProducts(
                page, size, keyword, category, brand, sellerId, authentication.getName());
        return ResponseEntity.ok(products);
    }

    // Paginated endpoint with totalElements and totalPages metadata
    @GetMapping("/paged")
    public ResponseEntity<PagedResponseDto<ProductResponseDto>> getProductsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long sellerId,
            Authentication authentication) {
        PagedResponseDto<ProductResponseDto> pagedProducts = productService.getProductsPaged(
                page, size, keyword, category, brand, sellerId, authentication.getName());
        return ResponseEntity.ok(pagedProducts);
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<ProductResponseDto>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        List<ProductResponseDto> products = productService.getMyProducts(page, size, authentication.getName());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(
            @PathVariable Long id,
            Authentication authentication) {
        ProductResponseDto response = productService.getProductById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto requestDto,
            Authentication authentication) {
        ProductResponseDto response = productService.updateProduct(id, requestDto, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponseDto> updateProductStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            Authentication authentication) {
        ProductResponseDto response = productService.updateProductStatus(id, active, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/variants/{variantId}/stock")
    public ResponseEntity<ProductResponseDto.VariantResponseDto> updateVariantStock(
            @PathVariable Long variantId,
            @Valid @RequestBody StockUpdateRequestDto requestDto,
            Authentication authentication) {

        ProductResponseDto.VariantResponseDto updatedVariant =
                productService.updateVariantStock(variantId, requestDto, authentication.getName());
        return ResponseEntity.ok(updatedVariant);
    }
}