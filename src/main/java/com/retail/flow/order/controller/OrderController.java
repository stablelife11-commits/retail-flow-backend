package com.retail.flow.order.controller;

import com.retail.flow.order.dto.OrderRequestDto;
import com.retail.flow.order.dto.OrderResponseDto;
import com.retail.flow.order.dto.SellerOrderUpdateRequestDto;
import com.retail.flow.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto requestDto,
            Authentication authentication) {
        OrderResponseDto response = orderService.createOrder(requestDto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponseDto>> createMultiSellerOrders(
            @Valid @RequestBody OrderRequestDto requestDto,
            Authentication authentication) {
        List<OrderResponseDto> responses = orderService.createMultiSellerOrders(requestDto, authentication.getName());
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<OrderResponseDto> orders = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        List<OrderResponseDto> orders = orderService.getMyOrders(authentication.getName(), page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersBySellerId(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        List<OrderResponseDto> orders = orderService.getOrdersBySellerId(sellerId, authentication.getName(), page, size);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> modifyOrderBySeller(
            @PathVariable Long id,
            @Valid @RequestBody SellerOrderUpdateRequestDto requestDto,
            Authentication authentication) {
        OrderResponseDto response = orderService.modifyOrderBySeller(id, requestDto, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        OrderResponseDto response = orderService.updateOrderStatus(id, status, authentication.getName());
        return ResponseEntity.ok(response);
    }
}