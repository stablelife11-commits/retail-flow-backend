package com.retail.flow.expense.service;

import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.expense.dto.ExpenseRequestDto;
import com.retail.flow.expense.dto.ExpenseResponseDto;
import com.retail.flow.expense.entity.Expense;
import com.retail.flow.expense.repository.ExpenseRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpenseResponseDto createExpense(ExpenseRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        Expense expense = Expense.builder()
                .sellerId(caller.getId())
                .amount(requestDto.getAmount())
                .expenseDate(requestDto.getExpenseDate())
                .category(requestDto.getCategory())
                .description(requestDto.getDescription())
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        return mapToResponseDto(savedExpense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponseDto> getAllExpenses(String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        List<Expense> expenses = (caller.getRole() == User.Role.ADMIN)
                ? expenseRepository.findAll()
                : expenseRepository.findBySellerId(caller.getId());

        return expenses.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
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