package com.retail.flow.returns.controller;

import com.retail.flow.returns.dto.ReturnRequestDto;
import com.retail.flow.returns.dto.ReturnResponseDto;
import com.retail.flow.returns.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnResponseDto> processReturn(
            @Valid @RequestBody ReturnRequestDto requestDto,
            Authentication authentication) {
        ReturnResponseDto response = returnService.processReturn(requestDto, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ReturnResponseDto>> getAllReturns(Authentication authentication) {
        List<ReturnResponseDto> returns = returnService.getAllReturns(authentication.getName());
        return ResponseEntity.ok(returns);
    }
}