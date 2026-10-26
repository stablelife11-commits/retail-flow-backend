package com.retail.flow.payment.controller;

import com.retail.flow.payment.dto.PaymentRequestDto;
import com.retail.flow.payment.dto.PaymentResponseDto;
import com.retail.flow.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> recordPayment(
            @Valid @RequestBody PaymentRequestDto requestDto,
            Authentication authentication) {
        PaymentResponseDto response = paymentService.recordPayment(requestDto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments(Authentication authentication) {
        List<PaymentResponseDto> payments = paymentService.getAllPayments(authentication.getName());
        return ResponseEntity.ok(payments);
    }
}