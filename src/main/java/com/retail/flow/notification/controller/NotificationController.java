package com.retail.flow.notification.controller;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.common.security.CurrentUserService;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.notification.entity.BuyerNotification;
import com.retail.flow.notification.repository.BuyerNotificationRepository;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.user.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final BuyerNotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;

    // 1. BUYER: अपने सारे नोटिफिकेशन प्राप्त करें
    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BuyerNotification>> getMyNotifications(Authentication authentication) {
        Customer customer = customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessValidationException("Buyer profile not found"));
        return ResponseEntity.ok(notificationRepository.findTop30ByCustomerIdOrderByIdDesc(customer.getId()));
    }

    // 2. BUYER: सारे नोटिफिकेशन को Read (पढ़ा हुआ) मार्क करें
    @PatchMapping("/mark-all-read")
    @Transactional
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        Customer customer = customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessValidationException("Buyer profile not found"));
        notificationRepository.markAllAsReadForCustomer(customer.getId());
        return ResponseEntity.noContent().build();
    }

    // 3. STRICTLY ADMIN ONLY: किसी ऑर्डर, किसी एक ग्राहक, या सभी ग्राहकों को नोटिफिकेशन भेजें
    @PostMapping("/send")
    @Transactional
    public ResponseEntity sendNotificationByAdmin(
            @Valid @RequestBody AdminNotificationRequest request,
            Authentication authentication) {
        User admin = currentUserService.getCurrentUser(authentication.getName());

        // अगर Admin सभी Buyers को एक साथ मैसेज भेजना चाहता है:
        if (Boolean.TRUE.equals(request.getSendToAllBuyers())) {
            List<Customer> allCustomers = customerRepository.findAll();
            List<BuyerNotification> batch = new ArrayList<>();
            for (Customer c : allCustomers) {
                if (Boolean.TRUE.equals(c.getActive())) {
                    batch.add(BuyerNotification.builder()
                            .customerId(c.getId())
                            .senderId(admin.getId())
                            .orderId(null)
                            .title(request.getTitle().trim())
                            .message(request.getMessage().trim())
                            .isRead(false)
                            .build());
                }
            }
            notificationRepository.saveAll(batch);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "sentCount", batch.size(),
                    "message", "Notification sent to all active buyers"
            ));
        }

        // अगर Admin किसी खास Order या खास Customer को भेजना चाहता है:
        Long targetCustomerId = request.getCustomerId();
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
            targetCustomerId = order.getCustomer().getId();
        }

        if (targetCustomerId == null) {
            throw new BusinessValidationException("Provide orderId, customerId, or set sendToAllBuyers = true.");
        }

        BuyerNotification saved = notificationRepository.save(BuyerNotification.builder()
                .customerId(targetCustomerId)
                .senderId(admin.getId())
                .orderId(request.getOrderId())
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .isRead(false)
                .build());

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "notification", saved
        ));
    }

    @Data
    public static class AdminNotificationRequest {
        private Long orderId;
        private Long customerId;
        private Boolean sendToAllBuyers = false;

        @NotBlank(message = "Notification title is required")
        private String title;

        @NotBlank(message = "Notification message is required")
        private String message;
    }
}