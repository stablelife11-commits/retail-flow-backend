package com.retail.flow.expense.service;

import com.retail.flow.expense.dto.ExpenseRequestDto;
import com.retail.flow.expense.dto.ExpenseResponseDto;
import com.retail.flow.expense.entity.Expense;
import com.retail.flow.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public ExpenseResponseDto createExpense(ExpenseRequestDto requestDto) {
        Expense expense = Expense.builder()
                .amount(requestDto.getAmount())
                .expenseDate(requestDto.getExpenseDate())
                .category(requestDto.getCategory())
                .description(requestDto.getDescription())
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        return mapToResponseDto(savedExpense);
    }

    public List<ExpenseResponseDto> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private ExpenseResponseDto mapToResponseDto(Expense expense) {
        return ExpenseResponseDto.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .build();
    }
}