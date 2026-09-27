package com.retail.flow.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PaymentResponseDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String referenceType;
    private Long referenceId;
    private String notes;
}