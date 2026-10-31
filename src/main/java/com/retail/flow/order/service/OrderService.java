package com.retail.flow.order.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.common.security.CurrentUserService;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
import com.retail.flow.order.dto.OrderItemRequestDto;
import com.retail.flow.order.dto.OrderRequestDto;
import com.retail.flow.order.dto.OrderResponseDto;
import com.retail.flow.order.dto.SellerOrderUpdateRequestDto;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.entity.Order.OrderStatus;
import com.retail.flow.order.entity.Order.PaymentStatus;
import com.retail.flow.order.entity.OrderItem;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final CurrentUserService currentUserService;

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

        Map<Long, Integer> mergedQuantities = new LinkedHashMap<>();
        Map<Long, String> mergedNotes = new HashMap<>();

        for (OrderItemRequestDto item : requestDto.getItems()) {
            mergedQuantities.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
            if (StringUtils.hasText(item.getItemNote())) {
                mergedNotes.merge(item.getVariantId(), item.getItemNote().trim(),
                        (oldNote, newNote) -> oldNote.equals(newNote) ? oldNote : oldNote + " | " + newNote);
            }
        }

        Map<Long, ProductVariant> variantMap = new HashMap<>();
        Map<Long, List> itemsBySeller = new LinkedHashMap<>();
        Map<Long, User> verifiedSellers = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : mergedQuantities.entrySet()) {
            Long variantId = entry.getKey();
            Integer requestedQty = entry.getValue();

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

            if (!Boolean.TRUE.equals(variant.getProduct().getActive())) {
                throw new BusinessValidationException("Product is no longer active: " + variant.getProduct().getName());
            }

            Long sellerId = variant.getProduct().getSellerId();
            User seller = verifiedSellers.computeIfAbsent(sellerId, id ->
                    userRepository.findById(id)
                            .orElseThrow(() -> new BusinessValidationException("Seller account not found for product: " + variant.getProduct().getName()))
            );

            if (!Boolean.TRUE.equals(seller.getActive())
                    || (seller.getRole() != User.Role.SELLER && seller.getRole() != User.Role.ADMIN)) {
                throw new BusinessValidationException("Seller for product '" + variant.getProduct().getName() + "' is currently inactive.");
            }

            int moq = variant.getMinimumOrderQuantity() != null ? variant.getMinimumOrderQuantity() : 1;
            int packSize = variant.getPackSize() != null ? variant.getPackSize() : 1;

            if (requestedQty < moq) {
                throw new BusinessValidationException("Minimum order quantity for SKU '" + variant.getSku()
                        + "' is " + moq + " units (Requested: " + requestedQty + ").");
            }

            if (packSize > 1 && requestedQty % packSize != 0) {
                throw new BusinessValidationException("Quantity for SKU '" + variant.getSku()
                        + "' must be ordered in multiples of pack size " + packSize + " (Requested: " + requestedQty + ").");
            }

            if (variant.getStock() < requestedQty) {
                throw new BusinessValidationException("Insufficient stock for SKU: " + variant.getSku()
                        + " (Available: " + variant.getStock() + ", Requested: " + requestedQty + ")");
            }

            variantMap.put(variantId, variant);
            itemsBySeller.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(entry);
        }

        if (!allowMultiSellerSplit && itemsBySeller.size() > 1) {
            throw new BusinessValidationException(
                    "Cart contains items from multiple sellers. Please use /api/v1/orders/checkout or place separate orders per seller.");
        }

        List<OrderResponseDto> responses = new ArrayList<>();

        for (Map.Entry<Long, List> sellerGroup : itemsBySeller.entrySet()) {
            Long sellerId = sellerGroup.getKey();
            List<Map.Entry<Long, Integer>> sellerItems = sellerGroup.getValue();

            Order order = Order.builder()
                    .customer(customer)
                    .sellerId(sellerId)
                    .orderItems(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .paymentStatus(PaymentStatus.UNPAID)
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
                        .itemNote(mergedNotes.get(variant.getId()))
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

            for (InventoryTransaction tx : pendingAuditLogs) {
                tx.setReferenceId(savedOrder.getId());
            }
            inventoryTransactionRepository.saveAll(pendingAuditLogs);

            responses.add(mapToResponseDto(savedOrder));
        }

        return responses;
    }

    @Transactional
    public OrderResponseDto modifyOrderBySeller(Long orderId, SellerOrderUpdateRequestDto requestDto, String callerEmail) {
        User caller = currentUserService.getCurrentUser(callerEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (caller.getRole() != User.Role.ADMIN && !order.getSellerId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to modify another seller's order.");
        }

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessValidationException(
                    "Order #" + orderId + " cannot be modified because it is already in " + order.getStatus() + " status.");
        }

        List<InventoryTransaction> inventoryLogs = new ArrayList<>();

        // 1. Restore previously reserved stock for all existing items in this order
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            for (OrderItem oldItem : order.getOrderItems()) {
                ProductVariant oldVariant = productVariantRepository.findById(oldItem.getProductVariant().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + oldItem.getProductVariant().getId()));
                int prevStock = oldVariant.getStock();
                int restoredStock = prevStock + oldItem.getQuantity();
                oldVariant.setStock(restoredStock);
                productVariantRepository.save(oldVariant);

                inventoryLogs.add(InventoryTransaction.builder()
                        .productVariant(oldVariant)
                        .type(InventoryTransaction.TransactionType.ORDER_RELEASE)
                        .quantity(oldItem.getQuantity())
                        .referenceType("ORDER_MODIFICATION_RELEASE")
                        .referenceId(order.getId())
                        .previousStock(prevStock)
                        .newStock(restoredStock)
                        .build());
            }
            order.getOrderItems().clear();
            orderRepository.flush();
        }

        // 2. Apply updated items, custom negotiated prices, and deduct stock
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequestDto itemReq : requestDto.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + itemReq.getVariantId()));

            if (!variant.getProduct().getSellerId().equals(order.getSellerId())) {
                throw new BusinessValidationException(
                        "Variant SKU '" + variant.getSku() + "' does not belong to your seller catalog.");
            }

            if (variant.getStock() < itemReq.getQuantity()) {
                throw new BusinessValidationException("Insufficient stock for SKU '" + variant.getSku()
                        + "' (Available: " + variant.getStock() + ", Requested: " + itemReq.getQuantity() + ").");
            }

            int prevStock = variant.getStock();
            int newStock = prevStock - itemReq.getQuantity();
            variant.setStock(newStock);
            productVariantRepository.save(variant);

            inventoryLogs.add(InventoryTransaction.builder()
                    .productVariant(variant)
                    .type(InventoryTransaction.TransactionType.ORDER_RESERVATION)
                    .quantity(itemReq.getQuantity())
                    .referenceType("ORDER_MODIFICATION_RESERVE")
                    .referenceId(order.getId())
                    .previousStock(prevStock)
                    .newStock(newStock)
                    .build());

            BigDecimal negotiatedUnitPrice = (itemReq.getUnitPrice() != null
                    && itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? itemReq.getUnitPrice()
                    : variant.getSellingPrice();

            BigDecimal lineTotal = negotiatedUnitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem newItem = OrderItem.builder()
                    .order(order)
                    .productVariant(variant)
                    .quantity(itemReq.getQuantity())
                    .price(negotiatedUnitPrice)
                    .itemNote(StringUtils.hasText(itemReq.getItemNote()) ? itemReq.getItemNote().trim() : null)
                    .build();

            order.getOrderItems().add(newItem);
        }

        // 3. Apply delivery address, discount, seller remarks, and recalculate total
        if (StringUtils.hasText(requestDto.getDeliveryAddress())) {
            order.setDeliveryAddress(requestDto.getDeliveryAddress().trim());
        }

        BigDecimal discount = (requestDto.getDiscountAmount() != null
                && requestDto.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                ? requestDto.getDiscountAmount()
                : BigDecimal.ZERO;

        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessValidationException(
                    "Discount amount (₹" + discount + ") cannot exceed order subtotal (₹" + subtotal + ").");
        }

        order.setDiscountAmount(discount);
        order.setSellerRemarks(StringUtils.hasText(requestDto.getSellerRemarks())
                ? requestDto.getSellerRemarks().trim()
                : null);

        BigDecimal finalTotal = subtotal.subtract(discount);
        order.setTotalAmount(finalTotal);

        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (paid.compareTo(finalTotal) >= 0) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        if (Boolean.TRUE.equals(requestDto.getConfirmImmediately()) && order.getStatus() == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.CONFIRMED);
        }

        Order savedOrder = orderRepository.save(order);
        if (!inventoryLogs.isEmpty()) {
            inventoryTransactionRepository.saveAll(inventoryLogs);
        }

        return mapToResponseDto(savedOrder);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, String newStatusStr, String callerEmail) {
        User caller = currentUserService.getCurrentUser(callerEmail);
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

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessValidationException(
                    "Invalid order state transition from " + currentStatus + " to " + targetStatus);
        }

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
    public List<OrderResponseDto> getAllOrders(int page, int size) {
        Pageable pageable = buildPageable(page, size);
        return orderRepository.findAll(pageable).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyOrders(String userEmail, int page, int size) {
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessValidationException("Customer profile not found"));
        Pageable pageable = buildPageable(page, size);
        return orderRepository.findByCustomerId(customer.getId(), pageable).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersBySellerId(Long sellerId, String callerEmail, int page, int size) {
        User caller = currentUserService.getCurrentUser(callerEmail);

        if (caller.getRole() != User.Role.ADMIN && !sellerId.equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to view another seller's orders.");
        }

        Pageable pageable = buildPageable(page, size);
        return orderRepository.findBySellerId(sellerId, pageable).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, Sort.by("id").descending());
    }

    private OrderResponseDto mapToResponseDto(Order order) {
        List<OrderResponseDto.OrderItemResponseDto> itemDtos = order.getOrderItems().stream()
                .map(item -> OrderResponseDto.OrderItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProductVariant().getProduct().getId())
                        .productName(item.getProductVariant().getProduct().getName())
                        .variantId(item.getProductVariant().getId())
                        .sku(item.getProductVariant().getSku())
                        .size(item.getProductVariant().getSize())
                        .color(item.getProductVariant().getColor())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .itemNote(item.getItemNote())
                        .build())
                .collect(Collectors.toList());

        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = total.add(discount);

        return OrderResponseDto.builder()
                .id(order.getId())
                .sellerId(order.getSellerId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .customerMobile(order.getCustomer().getMobile())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .subtotalAmount(subtotal)
                .discountAmount(discount)
                .totalAmount(total)
                .paidAmount(order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : PaymentStatus.UNPAID.name())
                .sellerRemarks(order.getSellerRemarks())
                .orderDate(order.getOrderDate())
                .items(itemDtos)
                .build();
    }
}