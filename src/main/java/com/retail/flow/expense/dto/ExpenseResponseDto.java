package com.retail.flow.expense.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ExpenseResponseDto {
    private Long id;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String category;
    private String description;
}