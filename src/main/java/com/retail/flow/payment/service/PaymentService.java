package com.retail.flow.payment.service;

import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.payment.dto.PaymentRequestDto;
import com.retail.flow.payment.dto.PaymentResponseDto;
import com.retail.flow.payment.entity.Payment;
import com.retail.flow.payment.repository.PaymentRepository;
import com.retail.flow.sale.entity.Sale;
import com.retail.flow.sale.repository.SaleRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public PaymentResponseDto recordPayment(PaymentRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        Customer customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + requestDto.getCustomerId()));

        // Validate referenced Sale or Order if provided
        if (requestDto.getReferenceId() != null && requestDto.getReferenceType() != null) {
            if ("SALE".equalsIgnoreCase(requestDto.getReferenceType())) {
                Sale sale = saleRepository.findById(requestDto.getReferenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Referenced Sale not found: " + requestDto.getReferenceId()));
                if (caller.getRole() != User.Role.ADMIN && !sale.getSellerId().equals(caller.getId())) {
                    throw new AccessDeniedException("Cannot record payment against another seller's sale.");
                }
            } else if ("ORDER".equalsIgnoreCase(requestDto.getReferenceType())) {
                Order order = orderRepository.findById(requestDto.getReferenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Referenced Order not found: " + requestDto.getReferenceId()));
                if (caller.getRole() != User.Role.ADMIN && !order.getSellerId().equals(caller.getId())) {
                    throw new AccessDeniedException("Cannot record payment against another seller's order.");
                }
            }
        }

        Payment payment = Payment.builder()
                .sellerId(caller.getId())
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

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments(String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        List<Payment> payments = (caller.getRole() == User.Role.ADMIN)
                ? paymentRepository.findAll()
                : paymentRepository.findBySellerId(caller.getId());

        return payments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
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