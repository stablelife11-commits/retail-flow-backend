package com.retail.flow.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardSummaryDto {
    private BigDecimal totalSalesAmount;
    private BigDecimal totalPurchaseAmount;
    private long totalCustomers;
    private long totalProducts;
    private long totalSuppliers;
}