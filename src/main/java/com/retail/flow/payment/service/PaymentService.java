package com.retail.flow.payment.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.entity.Order.OrderStatus;
import com.retail.flow.order.entity.Order.PaymentStatus;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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

        if (caller.getRole() != User.Role.ADMIN
                && !customerRepository.isCustomerAccessibleBySeller(customer.getId(), caller.getId())) {
            throw new AccessDeniedException("You are not authorized to record a payment for another seller's customer.");
        }

        String normalizedRefType = StringUtils.hasText(requestDto.getReferenceType())
                ? requestDto.getReferenceType().trim().toUpperCase()
                : "GENERAL";

        if (requestDto.getReferenceId() != null) {
            if ("SALE".equals(normalizedRefType)) {
                Sale sale = saleRepository.findById(requestDto.getReferenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Referenced Sale not found: " + requestDto.getReferenceId()));

                if (caller.getRole() != User.Role.ADMIN && !sale.getSellerId().equals(caller.getId())) {
                    throw new AccessDeniedException("Cannot record payment against another seller's sale.");
                }
                if (!sale.getCustomer().getId().equals(customer.getId())) {
                    throw new BusinessValidationException("Customer ID does not match the customer on the referenced Sale.");
                }

                BigDecimal currentPaid = sale.getPaidAmount() != null ? sale.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal remainingBalance = sale.getTotalAmount().subtract(currentPaid);

                if (requestDto.getAmount().compareTo(remainingBalance) > 0) {
                    throw new BusinessValidationException("Payment amount (" + requestDto.getAmount()
                            + ") exceeds remaining unpaid balance (" + remainingBalance + ") for Sale " + sale.getSaleNumber());
                }

                BigDecimal updatedPaid = currentPaid.add(requestDto.getAmount());
                sale.setPaidAmount(updatedPaid);
                sale.setPaymentStatus(updatedPaid.compareTo(sale.getTotalAmount()) >= 0
                        ? PaymentStatus.PAID
                        : PaymentStatus.PARTIALLY_PAID);
                saleRepository.save(sale);

            } else if ("ORDER".equals(normalizedRefType)) {
                Order order = orderRepository.findById(requestDto.getReferenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Referenced Order not found: " + requestDto.getReferenceId()));

                if (caller.getRole() != User.Role.ADMIN && !order.getSellerId().equals(caller.getId())) {
                    throw new AccessDeniedException("Cannot record payment against another seller's order.");
                }
                if (!order.getCustomer().getId().equals(customer.getId())) {
                    throw new BusinessValidationException("Customer ID does not match the customer on the referenced Order.");
                }
                if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
                    throw new BusinessValidationException("Cannot record payment for a " + order.getStatus() + " order.");
                }

                BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal remainingBalance = order.getTotalAmount().subtract(currentPaid);

                if (requestDto.getAmount().compareTo(remainingBalance) > 0) {
                    throw new BusinessValidationException("Payment amount (" + requestDto.getAmount()
                            + ") exceeds remaining unpaid balance (" + remainingBalance + ") for Order #" + order.getId());
                }

                BigDecimal updatedPaid = currentPaid.add(requestDto.getAmount());
                order.setPaidAmount(updatedPaid);
                order.setPaymentStatus(updatedPaid.compareTo(order.getTotalAmount()) >= 0
                        ? PaymentStatus.PAID
                        : PaymentStatus.PARTIALLY_PAID);
                orderRepository.save(order);

            } else if (!"GENERAL".equals(normalizedRefType)) {
                throw new BusinessValidationException("Invalid referenceType. Allowed values: SALE, ORDER, GENERAL.");
            }
        }

        Payment payment = Payment.builder()
                .sellerId(caller.getId())
                .customer(customer)
                .amount(requestDto.getAmount())
                .paymentDate(requestDto.getPaymentDate())
                .paymentMethod(requestDto.getPaymentMethod().trim().toUpperCase())
                .referenceType(normalizedRefType)
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