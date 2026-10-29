package com.retail.flow.cart.service;

import com.retail.flow.cart.dto.CartCheckoutRequestDto;
import com.retail.flow.cart.dto.CartItemRequestDto;
import com.retail.flow.cart.dto.CartResponseDto;
import com.retail.flow.cart.entity.Cart;
import com.retail.flow.cart.entity.CartItem;
import com.retail.flow.cart.repository.CartRepository;
import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.order.dto.OrderItemRequestDto;
import com.retail.flow.order.dto.OrderRequestDto;
import com.retail.flow.order.dto.OrderResponseDto;
import com.retail.flow.order.service.OrderService;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public CartResponseDto getMyCart(String callerEmail) {
        Customer customer = getBuyerCustomer(callerEmail);
        return cartRepository.findByCustomerId(customer.getId())
                .map(this::mapToResponseDto)
                .orElseGet(() -> CartResponseDto.builder()
                        .cartId(null)
                        .customerId(customer.getId())
                        .totalDistinctItems(0)
                        .totalQuantity(0)
                        .estimatedTotalAmount(BigDecimal.ZERO)
                        .updatedAt(null)
                        .items(List.of())
                        .build());
    }

    @Transactional
    public CartResponseDto addOrIncrementItem(CartItemRequestDto requestDto, String callerEmail) {
        Customer customer = getBuyerCustomer(callerEmail);
        Cart cart = getOrCreateCart(customer);

        ProductVariant variant = productVariantRepository.findById(requestDto.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + requestDto.getVariantId()));

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(ci -> ci.getProductVariant().getId().equals(variant.getId()))
                .findFirst();

        int targetQuantity = existingItemOpt
                .map(ci -> ci.getQuantity() + requestDto.getQuantity())
                .orElse(requestDto.getQuantity());

        validateVariantForCart(variant, targetQuantity);

        if (existingItemOpt.isPresent()) {
            existingItemOpt.get().setQuantity(targetQuantity);
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(targetQuantity)
                    .build());
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        return mapToResponseDto(savedCart);
    }

    @Transactional
    public CartResponseDto updateItemQuantity(Long variantId, Integer quantity, String callerEmail) {
        Customer customer = getBuyerCustomer(callerEmail);
        Cart cart = getOrCreateCart(customer);

        CartItem cartItem = cart.getItems().stream()
                .filter(ci -> ci.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant ID " + variantId + " is not in your cart."));

        if (quantity == null || quantity <= 0) {
            cart.getItems().remove(cartItem);
        } else {
            validateVariantForCart(cartItem.getProductVariant(), quantity);
            cartItem.setQuantity(quantity);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        return mapToResponseDto(savedCart);
    }

    @Transactional
    public CartResponseDto removeItem(Long variantId, String callerEmail) {
        Customer customer = getBuyerCustomer(callerEmail);
        Cart cart = getOrCreateCart(customer);

        boolean removed = cart.getItems().removeIf(ci -> ci.getProductVariant().getId().equals(variantId));
        if (!removed) {
            throw new ResourceNotFoundException("Variant ID " + variantId + " is not in your cart.");
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        return mapToResponseDto(savedCart);
    }

    @Transactional
    public void clearCart(String callerEmail) {
        Customer customer = getBuyerCustomer(callerEmail);
        cartRepository.findByCustomerId(customer.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
        });
    }

    @Transactional
    public List<OrderResponseDto> checkoutCart(CartCheckoutRequestDto checkoutDto, String callerEmail) {
        Customer customer = getBuyerCustomer(callerEmail);
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new BusinessValidationException("Your cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessValidationException("Your cart is empty.");
        }

        List<OrderItemRequestDto> orderItems = cart.getItems().stream()
                .map(ci -> {
                    OrderItemRequestDto itemDto = new OrderItemRequestDto();
                    itemDto.setVariantId(ci.getProductVariant().getId());
                    itemDto.setQuantity(ci.getQuantity());
                    return itemDto;
                })
                .collect(Collectors.toList());

        OrderRequestDto orderRequest = new OrderRequestDto();
        orderRequest.setDeliveryAddress(checkoutDto.getDeliveryAddress().trim());
        orderRequest.setItems(orderItems);

        // Atomically validates prices/MOQ/stock, splits multi-seller items into separate Orders, and deducts stock
        List<OrderResponseDto> createdOrders = orderService.createMultiSellerOrders(orderRequest, callerEmail);

        // Clear the persistent cart only after order creation succeeds
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return createdOrders;
    }

    private void validateVariantForCart(ProductVariant variant, int requestedQty) {
        if (!Boolean.TRUE.equals(variant.getProduct().getActive())) {
            throw new BusinessValidationException("Product '" + variant.getProduct().getName() + "' is no longer active.");
        }

        User seller = userRepository.findById(variant.getProduct().getSellerId())
                .orElseThrow(() -> new BusinessValidationException("Seller account not found."));

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
                    + "' must be a multiple of pack size " + packSize + " (Requested: " + requestedQty + ").");
        }

        if (variant.getStock() < requestedQty) {
            throw new BusinessValidationException("Insufficient stock for SKU '" + variant.getSku()
                    + "' (Available: " + variant.getStock() + ", Requested: " + requestedQty + ").");
        }
    }

    private Customer getBuyerCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessValidationException("Buyer customer profile not found for current user."));
    }

    private Cart getOrCreateCart(Customer customer) {
        return cartRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .customer(customer)
                        .items(new ArrayList<>())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    private CartResponseDto mapToResponseDto(Cart cart) {
        int totalQty = 0;
        BigDecimal estimatedTotal = BigDecimal.ZERO;
        List<CartResponseDto.CartItemResponseDto> itemDtos = new ArrayList<>();

        for (CartItem ci : cart.getItems()) {
            ProductVariant variant = ci.getProductVariant();
            BigDecimal lineTotal = variant.getSellingPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            totalQty += ci.getQuantity();
            estimatedTotal = estimatedTotal.add(lineTotal);

            itemDtos.add(CartResponseDto.CartItemResponseDto.builder()
                    .id(ci.getId())
                    .productId(variant.getProduct().getId())
                    .productName(variant.getProduct().getName())
                    .sellerId(variant.getProduct().getSellerId())
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .unitPrice(variant.getSellingPrice())
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .availableStock(variant.getStock())
                    .minimumOrderQuantity(variant.getMinimumOrderQuantity())
                    .packSize(variant.getPackSize())
                    .productActive(variant.getProduct().getActive())
                    .inStock(variant.getStock() >= ci.getQuantity())
                    .build());
        }

        return CartResponseDto.builder()
                .cartId(cart.getId())
                .customerId(cart.getCustomer().getId())
                .totalDistinctItems(itemDtos.size())
                .totalQuantity(totalQty)
                .estimatedTotalAmount(estimatedTotal)
                .updatedAt(cart.getUpdatedAt())
                .items(itemDtos)
                .build();
    }
}