package com.retail.flow.order.service;

import com.retail.flow.common.exception.BusinessValidationException;
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
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessValidationException("Customer profile not found"));

        Order order = Order.builder()
                .customer(customer)
                .orderItems(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .deliveryAddress(requestDto.getDeliveryAddress()) // 🟢 ADDED
                .status("PLACED") // 🟢 ADDED
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        Long sellerId = null;

        for (var itemDto : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new BusinessValidationException("Variant not found: " + itemDto.getVariantId()));

            if (variant.getStock() < itemDto.getQuantity()) {
                throw new BusinessValidationException("Insufficient stock for SKU: " + variant.getSku());
            }

            if (sellerId == null) {
                sellerId = variant.getProduct().getSellerId();
                order.setSellerId(sellerId);
            }

            variant.setStock(variant.getStock() - itemDto.getQuantity());
            productVariantRepository.save(variant);

            BigDecimal itemPrice = variant.getSellingPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemPrice);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productVariant(variant) // 🟢 FIX: Saving Variant directly
                    .quantity(itemDto.getQuantity())
                    .price(variant.getSellingPrice())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        return mapToResponseDto(savedOrder);
    }

    // 🟢 NAYA METHOD: Seller dwara order accept/confirm karne ke liye
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessValidationException("Order not found"));
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        return mapToResponseDto(savedOrder);
    }

    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    public List<OrderResponseDto> getMyOrders(String userEmail) {
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessValidationException("Customer profile not found"));
        return orderRepository.findByCustomerId(customer.getId()).stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }

    public List<OrderResponseDto> getOrdersBySellerId(Long sellerId) {
        return orderRepository.findBySellerId(sellerId).stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private OrderResponseDto mapToResponseDto(Order order) {
        List<OrderResponseDto.OrderItemResponseDto> itemDtos = order.getOrderItems().stream()
                .map(item -> OrderResponseDto.OrderItemResponseDto.builder()
                        .productId(item.getProductVariant().getProduct().getId())
                        .productName(item.getProductVariant().getProduct().getName())
                        .variantId(item.getProductVariant().getId())
                        .size(item.getProductVariant().getSize())
                        .color(item.getProductVariant().getColor())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .customerMobile(order.getCustomer().getMobile()) // 🟢 ADDED
                .status(order.getStatus()) // 🟢 ADDED
                .deliveryAddress(order.getDeliveryAddress()) // 🟢 ADDED
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .items(itemDtos)
                .build();
    }
}