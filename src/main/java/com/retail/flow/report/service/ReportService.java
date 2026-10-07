package com.retail.flow.report.service;

import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.purchase.repository.PurchaseRepository;
import com.retail.flow.report.dto.DashboardSummaryDto;
import com.retail.flow.sale.repository.SaleRepository;
import com.retail.flow.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        // 🟢 PERFORMANCE FIX: No more memory overflow. Fast DB aggregation.
        BigDecimal totalSales = saleRepository.sumTotalSales();
        BigDecimal totalPurchases = purchaseRepository.sumTotalPurchases();

        return DashboardSummaryDto.builder()
                .totalSalesAmount(totalSales)
                .totalPurchaseAmount(totalPurchases)
                .totalCustomers(customerRepository.count())
                .totalProducts(productRepository.count())
                .totalSuppliers(supplierRepository.count())
                .build();
    }
}