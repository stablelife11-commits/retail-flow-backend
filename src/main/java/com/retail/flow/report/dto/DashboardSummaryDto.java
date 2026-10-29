package com.retail.flow.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardSummaryDto {
    // Net combined revenue: (posSalesAmount + onlineOrderSalesAmount) - totalReturnsAmount
    private BigDecimal totalSalesAmount;
    private BigDecimal posSalesAmount;
    private BigDecimal onlineOrderSalesAmount;
    private BigDecimal totalReturnsAmount;
    private BigDecimal totalPurchaseAmount;
    private BigDecimal totalExpensesAmount;
    private long totalCustomers;
    private long totalProducts;
    private long totalSuppliers;
}