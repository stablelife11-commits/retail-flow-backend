package com.retail.flow.returns.controller;

import com.retail.flow.returns.dto.ReturnRequestDto;
import com.retail.flow.returns.dto.ReturnResponseDto;
import com.retail.flow.returns.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnResponseDto> processReturn(@Valid @RequestBody ReturnRequestDto requestDto) {
        ReturnResponseDto response = returnService.processReturn(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}