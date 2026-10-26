package com.retail.flow.common.config;

import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductVariantRepository productVariantRepository;

    @Value("${app.admin.default-email:admin@retailflow.com}")
    private String defaultAdminEmail;

    @Value("${app.admin.default-password:ChangeMeImmediately!123}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        try {
            productVariantRepository.fixNullVersions();
            log.info("Database Versioning Synchronized Successfully");
        } catch (Exception e) {
            log.warn("Versioning Fix Skipped: {}", e.getMessage());
        }

        if (!userRepository.existsByRole(User.Role.ADMIN)) {
            User admin = User.builder()
                    .name("System Administrator")
                    .email(defaultAdminEmail)
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .role(User.Role.ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("Default Admin account initialized for email: {}", defaultAdminEmail);
        }
    }
}