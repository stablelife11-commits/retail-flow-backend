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
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        // 1. Validate Customer
        Customer customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + requestDto.getCustomerId()));

        Order order = Order.builder()
                .customer(customer)
                .orderItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        Long sellerId = null; // 🟢 नया: सेलर आईडी स्टोर करने के लिए

        // 2. Process Order Items & Check Variant Stock
        for (var itemDto : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + itemDto.getVariantId()));

            if (variant.getStock() < itemDto.getQuantity()) {
                throw new RuntimeException("Insufficient stock for SKU: " + variant.getSku());
            }

            // 🟢 नया: पहले आइटम से प्रोडक्ट का सेलर निकालकर ऑर्डर में सेव कर दें
            if (sellerId == null) {
                sellerId = variant.getProduct().getSellerId();
                order.setSellerId(sellerId);
            }

            // Reduce variant stock
            variant.setStock(variant.getStock() - itemDto.getQuantity());
            productVariantRepository.save(variant);

            // Calculate item total price using selling price
            BigDecimal itemPrice = variant.getSellingPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemPrice);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(variant.getProduct()) // Parent product reference
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

    // 🟢 नया मेथड: जो OrderController में सेलर के लिए कॉल होगा
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