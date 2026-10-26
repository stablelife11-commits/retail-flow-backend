package com.retail.flow.customer.controller;

import com.retail.flow.customer.dto.CustomerRequestDto;
import com.retail.flow.customer.dto.CustomerResponseDto;
import com.retail.flow.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CustomerRequestDto requestDto,
            Authentication authentication) {
        CustomerResponseDto response = customerService.createCustomer(requestDto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers(Authentication authentication) {
        List<CustomerResponseDto> customers = customerService.getAllCustomers(authentication.getName());
        return ResponseEntity.ok(customers);
    }
}