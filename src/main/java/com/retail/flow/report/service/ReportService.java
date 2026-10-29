package com.retail.flow.report.service;

import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.expense.repository.ExpenseRepository;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.purchase.repository.PurchaseRepository;
import com.retail.flow.report.dto.DashboardSummaryDto;
import com.retail.flow.returns.repository.SaleReturnRepository;
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
    private final OrderRepository orderRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (caller.getRole() == User.Role.ADMIN) {
            BigDecimal posSales = saleRepository.sumTotalSales();
            BigDecimal onlineSales = orderRepository.sumDeliveredOrders();
            BigDecimal totalReturns = saleReturnRepository.sumTotalReturns();
            BigDecimal netSales = posSales.add(onlineSales).subtract(totalReturns);

            return DashboardSummaryDto.builder()
                    .totalSalesAmount(netSales)
                    .posSalesAmount(posSales)
                    .onlineOrderSalesAmount(onlineSales)
                    .totalReturnsAmount(totalReturns)
                    .totalPurchaseAmount(purchaseRepository.sumTotalPurchases())
                    .totalExpensesAmount(expenseRepository.sumTotalExpenses())
                    .totalCustomers(customerRepository.count())
                    .totalProducts(productRepository.count())
                    .totalSuppliers(supplierRepository.count())
                    .build();
        }

        Long sellerId = caller.getId();
        BigDecimal sellerPosSales = saleRepository.sumTotalSalesBySellerId(sellerId);
        BigDecimal sellerOnlineSales = orderRepository.sumDeliveredOrdersBySellerId(sellerId);
        BigDecimal sellerReturns = saleReturnRepository.sumTotalReturnsBySellerId(sellerId);
        BigDecimal sellerNetSales = sellerPosSales.add(sellerOnlineSales).subtract(sellerReturns);

        return DashboardSummaryDto.builder()
                .totalSalesAmount(sellerNetSales)
                .posSalesAmount(sellerPosSales)
                .onlineOrderSalesAmount(sellerOnlineSales)
                .totalReturnsAmount(sellerReturns)
                .totalPurchaseAmount(purchaseRepository.sumTotalPurchasesBySellerId(sellerId))
                .totalExpensesAmount(expenseRepository.sumTotalExpensesBySellerId(sellerId))
                .totalCustomers(customerRepository.countDistinctCustomersForSeller(sellerId))
                .totalProducts(productRepository.countBySellerId(sellerId))
                .totalSuppliers(supplierRepository.countBySellerId(sellerId))
                .build();
    }
}