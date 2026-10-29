package com.retail.flow.cart.controller;

import com.retail.flow.cart.dto.CartCheckoutRequestDto;
import com.retail.flow.cart.dto.CartItemRequestDto;
import com.retail.flow.cart.dto.CartResponseDto;
import com.retail.flow.cart.service.CartService;
import com.retail.flow.order.dto.OrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDto> getMyCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getMyCart(authentication.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addItemToCart(
            @Valid @RequestBody CartItemRequestDto requestDto,
            Authentication authentication) {
        return ResponseEntity.ok(cartService.addOrIncrementItem(requestDto, authentication.getName()));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @PathVariable Long variantId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        return ResponseEntity.ok(cartService.updateItemQuantity(variantId, quantity, authentication.getName()));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartResponseDto> removeCartItem(
            @PathVariable Long variantId,
            Authentication authentication) {
        return ResponseEntity.ok(cartService.removeItem(variantId, authentication.getName()));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponseDto>> checkoutCart(
            @Valid @RequestBody CartCheckoutRequestDto requestDto,
            Authentication authentication) {
        List<OrderResponseDto> orders = cartService.checkoutCart(requestDto, authentication.getName());
        return new ResponseEntity<>(orders, HttpStatus.CREATED);
    }
}