package com.retail.flow.order.service;

import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.order.dto.OrderRequestDto;
import com.retail.flow.order.dto.OrderResponseDto;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.entity.OrderItem;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto, String userEmail) {
        // 🟢 SECURITY FIX: We completely ignore requestDto.getCustomerId()
        // We fetch the true customer based on the mathematically verified JWT Token (userEmail).
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for email: " + userEmail));

        Order order = Order.builder()
                .customer(customer)
                .orderItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        Long sellerId = null;

        for (var itemDto : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + itemDto.getVariantId()));

            if (variant.getStock() < itemDto.getQuantity()) {
                throw new RuntimeException("Insufficient stock for SKU: " + variant.getSku());
            }

            if (sellerId == null) {
                sellerId = variant.getProduct().getSellerId();
                order.setSellerId(sellerId);
            }

            // Note: Inline stock deduction (Will be fixed properly in the Concurrency Phase with Optimistic Locking)
            variant.setStock(variant.getStock() - itemDto.getQuantity());
            productVariantRepository.save(variant);

            BigDecimal itemPrice = variant.getSellingPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemPrice);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(variant.getProduct())
                    .quantity(itemDto.getQuantity())
                    .price(variant.getSellingPrice())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        return mapToResponseDto(savedOrder);
    }

    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // 🟢 SECURITY FIX: Secure method to get ONLY the logged-in customer's orders
    public List<OrderResponseDto> getMyOrders(String userEmail) {
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for email: " + userEmail));

        return orderRepository.findByCustomerId(customer.getId()).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDto> getOrdersBySellerId(Long sellerId) {
        return orderRepository.findBySellerId(sellerId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private OrderResponseDto mapToResponseDto(Order order) {
        List<OrderResponseDto.OrderItemResponseDto> itemDtos = order.getOrderItems().stream()
                .map(item -> OrderResponseDto.OrderItemResponseDto.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .items(itemDtos)
                .build();
    }
}