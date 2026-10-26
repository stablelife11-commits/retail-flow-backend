package com.retail.flow.order.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
import com.retail.flow.order.dto.OrderItemRequestDto;
import com.retail.flow.order.dto.OrderRequestDto;
import com.retail.flow.order.dto.OrderResponseDto;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.entity.Order.OrderStatus;
import com.retail.flow.order.entity.OrderItem;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto, String userEmail) {
        List<OrderResponseDto> createdOrders = processOrdersBySeller(requestDto, userEmail, false);
        return createdOrders.get(0);
    }

    @Transactional
    public List<OrderResponseDto> createMultiSellerOrders(OrderRequestDto requestDto, String userEmail) {
        return processOrdersBySeller(requestDto, userEmail, true);
    }

    private List<OrderResponseDto> processOrdersBySeller(
            OrderRequestDto requestDto, String userEmail, boolean allowMultiSellerSplit) {

        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessValidationException("Customer profile not found for current user"));

        // 1. Merge duplicate variantIds in the same request payload
        Map<Long, Integer> mergedQuantities = new LinkedHashMap<>();
        for (OrderItemRequestDto item : requestDto.getItems()) {
            mergedQuantities.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
        }

        // 2. Load and validate all variants and group by sellerId
        Map<Long, ProductVariant> variantMap = new HashMap<>();
        Map<Long, List<Map.Entry<Long, Integer>>> itemsBySeller = new LinkedHashMap<>();

        for (Map.Entry<Long, Integer> entry : mergedQuantities.entrySet()) {
            Long variantId = entry.getKey();
            Integer requestedQty = entry.getValue();

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

            if (!Boolean.TRUE.equals(variant.getProduct().getActive())) {
                throw new BusinessValidationException("Product is no longer active: " + variant.getProduct().getName());
            }

            if (variant.getStock() < requestedQty) {
                throw new BusinessValidationException("Insufficient stock for SKU: " + variant.getSku()
                        + " (Available: " + variant.getStock() + ", Requested: " + requestedQty + ")");
            }

            variantMap.put(variantId, variant);
            Long sellerId = variant.getProduct().getSellerId();
            itemsBySeller.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(entry);
        }

        if (!allowMultiSellerSplit && itemsBySeller.size() > 1) {
            throw new BusinessValidationException(
                    "Cart contains items from multiple sellers. Please use /api/v1/orders/checkout or place separate orders per seller.");
        }

        // 3. Create one Order per Seller and log InventoryTransactions with the saved Order ID
        List<OrderResponseDto> responses = new ArrayList<>();

        for (Map.Entry<Long, List<Map.Entry<Long, Integer>>> sellerGroup : itemsBySeller.entrySet()) {
            Long sellerId = sellerGroup.getKey();
            List<Map.Entry<Long, Integer>> sellerItems = sellerGroup.getValue();

            Order order = Order.builder()
                    .customer(customer)
                    .sellerId(sellerId)
                    .orderItems(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .deliveryAddress(requestDto.getDeliveryAddress())
                    .status(OrderStatus.PLACED)
                    .build();

            BigDecimal totalAmount = BigDecimal.ZERO;
            List<InventoryTransaction> pendingAuditLogs = new ArrayList<>();

            for (Map.Entry<Long, Integer> itemEntry : sellerItems) {
                ProductVariant variant = variantMap.get(itemEntry.getKey());
                int quantity = itemEntry.getValue();

                int previousStock = variant.getStock();
                int newStock = previousStock - quantity;

                variant.setStock(newStock);
                productVariantRepository.save(variant);

                BigDecimal lineTotal = variant.getSellingPrice().multiply(BigDecimal.valueOf(quantity));
                totalAmount = totalAmount.add(lineTotal);

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .productVariant(variant)
                        .quantity(quantity)
                        .price(variant.getSellingPrice())
                        .build();

                order.getOrderItems().add(orderItem);

                pendingAuditLogs.add(InventoryTransaction.builder()
                        .productVariant(variant)
                        .type(InventoryTransaction.TransactionType.ORDER_RESERVATION)
                        .quantity(quantity)
                        .referenceType("ORDER")
                        .previousStock(previousStock)
                        .newStock(newStock)
                        .build());
            }

            order.setTotalAmount(totalAmount);
            Order savedOrder = orderRepository.save(order);

            // Attach the generated Order ID to each InventoryTransaction
            for (InventoryTransaction tx : pendingAuditLogs) {
                tx.setReferenceId(savedOrder.getId());
            }
            inventoryTransactionRepository.saveAll(pendingAuditLogs);

            responses.add(mapToResponseDto(savedOrder));
        }

        return responses;
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, String newStatusStr, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(newStatusStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Invalid order status: " + newStatusStr
                    + ". Allowed values: " + Arrays.toString(OrderStatus.values()));
        }

        OrderStatus currentStatus = order.getStatus();

        // Authorization checks based on Role
        if (caller.getRole() == User.Role.BUYER) {
            Customer buyerCustomer = customerRepository.findByEmail(callerEmail)
                    .orElseThrow(() -> new AccessDeniedException("Customer profile not found"));

            if (!order.getCustomer().getId().equals(buyerCustomer.getId())) {
                throw new AccessDeniedException("You can only update your own orders.");
            }
            if (targetStatus != OrderStatus.CANCELLED || currentStatus != OrderStatus.PLACED) {
                throw new BusinessValidationException("Buyers can only cancel an order while it is still in PLACED status.");
            }
        } else if (caller.getRole() == User.Role.SELLER) {
            if (!order.getSellerId().equals(caller.getId())) {
                throw new AccessDeniedException("You are not authorized to update another seller's order.");
            }
        }

        // State machine transition check
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessValidationException(
                    "Invalid order state transition from " + currentStatus + " to " + targetStatus);
        }

        // Restore inventory if transitioning to CANCELLED or REJECTED
        if (targetStatus.isStockReleaseState()) {
            restoreInventoryForOrder(order);
        }

        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);
        return mapToResponseDto(savedOrder);
    }

    private void restoreInventoryForOrder(Order order) {
        List<InventoryTransaction> releaseLogs = new ArrayList<>();

        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = item.getProductVariant();
            int previousStock = variant.getStock();
            int newStock = previousStock + item.getQuantity();

            variant.setStock(newStock);
            productVariantRepository.save(variant);

            releaseLogs.add(InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.ORDER_RELEASE)
                    .quantity(item.getQuantity())
                    .referenceType("ORDER")
                    .referenceId(order.getId())
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build());
        }

        inventoryTransactionRepository.saveAll(releaseLogs);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyOrders(String userEmail) {
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessValidationException("Customer profile not found"));
        return orderRepository.findByCustomerId(customer.getId()).stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersBySellerId(Long sellerId, String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        if (caller.getRole() != User.Role.ADMIN && !sellerId.equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to view another seller's orders.");
        }

        return orderRepository.findBySellerId(sellerId).stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
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
                .customerMobile(order.getCustomer().getMobile())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .items(itemDtos)
                .build();
    }
}