package com.retail.flow.common.config;

import com.retail.flow.banner.entity.AppBanner;
import com.retail.flow.banner.repository.AppBannerRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import com.retail.flow.banner.entity.AppBanner;
import com.retail.flow.banner.repository.AppBannerRepository;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String INSECURE_DEFAULT_ADMIN_PASSWORD = "ChangeMeImmediately!123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    private final AppBannerRepository appBannerRepository;

    @Value("${app.admin.default-email:admin@retailflow.com}")
    private String defaultAdminEmail;

    @Value("${app.admin.default-password:ChangeMeImmediately!123}")
    private String defaultAdminPassword;

    @PostConstruct
    public void validateAdminCredentials() {
        boolean isProdProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProdProfile) {
            if (defaultAdminPassword == null
                    || defaultAdminPassword.trim().length() < 8
                    || INSECURE_DEFAULT_ADMIN_PASSWORD.equals(defaultAdminPassword.trim())) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY ERROR: Production profile ('prod') requires a strong ADMIN_PASSWORD (min 8 chars) and cannot use the default sample password.");
            }
        }
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(User.Role.ADMIN)) {
            User admin = User.builder()
                    .name("System Administrator")
                    .email(defaultAdminEmail.trim().toLowerCase())
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .role(User.Role.ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("Default Admin account initialized for email: {}", defaultAdminEmail);
        }
        if (appBannerRepository.count() == 0) {
            appBannerRepository.saveAll(List.of(
                    AppBanner.builder()
                            .tag("WHOLESALE MEGA MELA")
                            .title("Amul Comfy All Products")
                            .subtitle("Up to 25% margin on bulk innerwear & apparel packs")
                            .buttonText("Explore Lots →")
                            .bannerType("HOME_CAROUSEL")
                            .actionType("ALL_PRODUCTS")
                            .displayOrder(1)
                            .active(true)
                            .build(),
                    AppBanner.builder()
                            .tag("DIRECT MILL DISPATCH")
                            .title("Dixcy Scott All Products")
                            .subtitle("Order in set-wise packs directly from verified sellers")
                            .buttonText("Explore Lots →")
                            .bannerType("HOME_CAROUSEL")
                            .actionType("ALL_PRODUCTS")
                            .displayOrder(2)
                            .active(true)
                            .build(),
                    AppBanner.builder()
                            .tag("GST INVOICE READY")
                            .title("Ladies and Gents Garments")
                            .subtitle("Negotiate custom rates with sellers before confirmation")
                            .buttonText("Explore Lots →")
                            .bannerType("HOME_CAROUSEL")
                            .actionType("ALL_PRODUCTS")
                            .displayOrder(3)
                            .active(true)
                            .build()
            ));
        }
    }
}