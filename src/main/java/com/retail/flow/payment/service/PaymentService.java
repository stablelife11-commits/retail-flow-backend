package com.retail.flow.payment.service;

import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.payment.dto.PaymentRequestDto;
import com.retail.flow.payment.dto.PaymentResponseDto;
import com.retail.flow.payment.entity.Payment;
import com.retail.flow.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public PaymentResponseDto recordPayment(PaymentRequestDto requestDto) {
        Customer customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + requestDto.getCustomerId()));

        Payment payment = Payment.builder()
                .customer(customer)
                .amount(requestDto.getAmount())
                .paymentDate(requestDto.getPaymentDate())
                .paymentMethod(requestDto.getPaymentMethod())
                .referenceType(requestDto.getReferenceType())
                .referenceId(requestDto.getReferenceId())
                .notes(requestDto.getNotes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return mapToResponseDto(savedPayment);
    }

    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private PaymentResponseDto mapToResponseDto(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .customerId(payment.getCustomer().getId())
                .customerName(payment.getCustomer().getName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .referenceType(payment.getReferenceType())
                .referenceId(payment.getReferenceId())
                .notes(payment.getNotes())
                .build();
    }
}