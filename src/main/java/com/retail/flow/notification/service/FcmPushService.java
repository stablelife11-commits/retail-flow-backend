package com.retail.flow.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FcmPushService {

    @Value("${FIREBASE_CREDENTIALS_JSON:}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void initFirebase() {
        try {
            if (!StringUtils.hasText(firebaseCredentialsJson)) {
                log.info("FIREBASE_CREDENTIALS_JSON is not set. FCM Push is disabled; DB notifications remain active.");
                return;
            }
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(firebaseCredentialsJson.trim().getBytes(StandardCharsets.UTF_8))
                );
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully!");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }

    // 1. Buyer को Push Notification भेजने के लिए
    public void sendPushToBuyer(String fcmToken, String title, String body, Long orderId) {
        sendPushInternal(fcmToken, title, body, orderId, "BUYER");
    }

    // 2. Seller को Push Notification भेजने के लिए
    public void sendPushToSeller(String fcmToken, String title, String body, Long orderId) {
        sendPushInternal(fcmToken, title, body, orderId, "SELLER");
    }

    // 3. Admin को Push Notification भेजने के लिए
    public void sendPushToAdmin(String fcmToken, String title, String body, Long orderId) {
        sendPushInternal(fcmToken, title, body, orderId, "ADMIN");
    }

    private void sendPushInternal(String fcmToken, String title, String body, Long orderId, String targetRole) {
        if (!StringUtils.hasText(fcmToken)) {
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Message message = Message.builder()
                        .setToken(fcmToken.trim())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("title", title)
                        .putData("message", body)
                        .putData("targetRole", targetRole)
                        .putData("orderId", orderId != null ? String.valueOf(orderId) : "")
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setChannelId("flowmart_order_updates")
                                        .setSound("default")
                                        .setDefaultVibrateTimings(true)
                                        .build())
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (Exception e) {
                log.warn("FCM push delivery failed for {} (Order #{}): {}", targetRole, orderId, e.getMessage());
            }
        });
    }
}