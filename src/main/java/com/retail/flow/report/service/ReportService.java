package com.retail.flow.report.service;

import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.purchase.repository.PurchaseRepository;
import com.retail.flow.report.dto.DashboardSummaryDto;
import com.retail.flow.sale.repository.SaleRepository;
import com.retail.flow.supplier.repository.SupplierRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (caller.getRole() == User.Role.ADMIN) {
            return DashboardSummaryDto.builder()
                    .totalSalesAmount(saleRepository.sumTotalSales())
                    .totalPurchaseAmount(purchaseRepository.sumTotalPurchases())
                    .totalCustomers(customerRepository.count())
                    .totalProducts(productRepository.count())
                    .totalSuppliers(supplierRepository.count())
                    .build();
        }

        Long sellerId = caller.getId();
        BigDecimal sellerSales = saleRepository.sumTotalSalesBySellerId(sellerId);
        BigDecimal sellerPurchases = purchaseRepository.sumTotalPurchasesBySellerId(sellerId);

        return DashboardSummaryDto.builder()
                .totalSalesAmount(sellerSales)
                .totalPurchaseAmount(sellerPurchases)
                .totalCustomers(customerRepository.countDistinctCustomersForSeller(sellerId))
                .totalProducts(productRepository.countBySellerId(sellerId))
                .totalSuppliers(supplierRepository.countBySellerId(sellerId))
                .build();
    }
}