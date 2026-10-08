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
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto requestDto, Authentication authentication) {
        // 🟢 Pass the verified email from the JWT token to the service
        OrderResponseDto response = orderService.createOrder(requestDto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // 🟢 SECURITY FIX: Secure endpoint for Customer App
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(Authentication authentication) {
        List<OrderResponseDto> orders = orderService.getMyOrders(authentication.getName());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersBySellerId(@PathVariable Long sellerId) {
        List<OrderResponseDto> orders = orderService.getOrdersBySellerId(sellerId);
        return ResponseEntity.ok(orders);
    }
    // Controller me existing code ke sath yeh method jod dein:
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        OrderResponseDto response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(response);
    }
}