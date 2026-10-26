package com.retail.flow.order.controller;

import com.retail.flow.order.dto.OrderRequestDto;
import com.retail.flow.order.dto.OrderResponseDto;
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

    // Multi-seller cart checkout: Automatically splits items by sellerId into separate orders
    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponseDto>> createMultiSellerOrders(
            @Valid @RequestBody OrderRequestDto requestDto,
            Authentication authentication) {
        List<OrderResponseDto> responses = orderService.createMultiSellerOrders(requestDto, authentication.getName());
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(Authentication authentication) {
        List<OrderResponseDto> orders = orderService.getMyOrders(authentication.getName());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersBySellerId(
            @PathVariable Long sellerId,
            Authentication authentication) {
        List<OrderResponseDto> orders = orderService.getOrdersBySellerId(sellerId, authentication.getName());
        return ResponseEntity.ok(orders);
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