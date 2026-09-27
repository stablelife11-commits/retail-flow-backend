package com.retail.flow.report.service;

import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.purchase.repository.PurchaseRepository;
import com.retail.flow.report.dto.DashboardSummaryDto;
import com.retail.flow.sale.repository.SaleRepository;
import com.retail.flow.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    public DashboardSummaryDto getDashboardSummary() {
        // Calculate Total Sales
        BigDecimal totalSales = saleRepository.findAll().stream()
                .map(sale -> sale.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Total Purchases
        BigDecimal totalPurchases = purchaseRepository.findAll().stream()
                .map(purchase -> purchase.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryDto.builder()
                .totalSalesAmount(totalSales)
                .totalPurchaseAmount(totalPurchases)
                .totalCustomers(customerRepository.count())
                .totalProducts(productRepository.count())
                .totalSuppliers(supplierRepository.count())
                .build();
    }
}