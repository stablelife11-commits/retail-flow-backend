package com.retail.flow.notification.controller;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.common.security.CurrentUserService;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.notification.entity.BuyerNotification;
import com.retail.flow.notification.repository.BuyerNotificationRepository;
import com.retail.flow.notification.service.FcmPushService;
import com.retail.flow.order.entity.Order;
import com.retail.flow.order.repository.OrderRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final BuyerNotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FcmPushService fcmPushService;

    public NotificationController(
            BuyerNotificationRepository notificationRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            FcmPushService fcmPushService) {
        this.notificationRepository = notificationRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fcmPushService = fcmPushService;
    }

    // 1. BUYER: अपना FCM Device Token रजिस्टर करें
    @PostMapping("/fcm-token")
    @Transactional
    public ResponseEntity<Void> updateFcmToken(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String token = body != null ? body.get("fcmToken") : null;
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.badRequest().build();
        }
        customerRepository.findByEmail(authentication.getName()).ifPresent(customer -> {
            customer.setFcmToken(token.trim());
            customerRepository.save(customer);
        });
        return ResponseEntity.ok().build();
    }

    // 2. BUYER: अपने सारे नोटिफिकेशन प्राप्त करें
    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BuyerNotification>> getMyNotifications(Authentication authentication) {
        Customer customer = customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessValidationException("Buyer profile not found"));
        return ResponseEntity.ok(notificationRepository.findTop30ByCustomerIdOrderByIdDesc(customer.getId()));
    }

    // 3. BUYER: सारे नोटिफिकेशन को Read मार्क करें
    @PatchMapping("/mark-all-read")
    @Transactional
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        Customer customer = customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessValidationException("Buyer profile not found"));
        notificationRepository.markAllAsReadForCustomer(customer.getId());
        return ResponseEntity.noContent().build();
    }

    // 4. STRICTLY ADMIN ONLY: किसी ऑर्डर, ग्राहक या सभी ग्राहकों को तुरंत Push Notification भेजें
    @PostMapping("/send")
    @Transactional
    public ResponseEntity sendNotificationByAdmin(
            @Valid @RequestBody AdminNotificationRequest request,
            Authentication authentication) {
        User admin = currentUserService.getCurrentUser(authentication.getName());
        String cleanTitle = request.getTitle().trim();
        String cleanMsg = request.getMessage().trim();

        // A. सभी दुकानदारों (All Buyers) के लिए
        if (Boolean.TRUE.equals(request.getSendToAllBuyers())) {
            List<Customer> allCustomers = customerRepository.findAll();
            List<BuyerNotification> batch = new ArrayList<>();
            for (Customer c : allCustomers) {
                if (Boolean.TRUE.equals(c.getActive())) {
                    batch.add(BuyerNotification.builder()
                            .customerId(c.getId())
                            .senderId(admin.getId())
                            .orderId(null)
                            .title(cleanTitle)
                            .message(cleanMsg)
                            .isRead(false)
                            .build());
                    fcmPushService.sendPushToBuyer(c.getFcmToken(), cleanTitle, cleanMsg, null);
                }
            }
            notificationRepository.saveAll(batch);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "sentCount", batch.size(),
                    "message", "Notification sent to all active buyers"
            ));
        }

        // B. खास ऑर्डर (Order ID) या एक ग्राहक (Customer ID / User ID) के लिए
        Customer targetCustomer = null;
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order #" + request.getOrderId() + " नहीं मिला।"));
            targetCustomer = order.getCustomer();
        } else if (request.getCustomerId() != null) {
            Long inputId = request.getCustomerId();
            // पहले customers टेबल में ढूँढें, अगर न मिले तो users टेबल की ID से ईमेल निकालकर ढूँढें
            targetCustomer = customerRepository.findById(inputId)
                    .orElseGet(() -> userRepository.findById(inputId)
                            .flatMap(u -> customerRepository.findByEmail(u.getEmail()))
                            .orElseThrow(() -> new ResourceNotFoundException("Customer/User ID #" + inputId + " नहीं मिला।")));
        }

        if (targetCustomer == null) {
            throw new BusinessValidationException("कृपया Order ID या Customer ID प्रदान करें।");
        }

        BuyerNotification saved = notificationRepository.save(BuyerNotification.builder()
                .customerId(targetCustomer.getId())
                .senderId(admin.getId())
                .orderId(request.getOrderId())
                .title(cleanTitle)
                .message(cleanMsg)
                .isRead(false)
                .build());

        fcmPushService.sendPushToBuyer(targetCustomer.getFcmToken(), cleanTitle, cleanMsg, request.getOrderId());

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