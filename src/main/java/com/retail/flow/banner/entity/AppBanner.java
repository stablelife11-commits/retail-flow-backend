package com.retail.flow.banner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_banners", indexes = {
        @Index(name = "idx_app_banners_active_order", columnList = "active, display_order"),
        @Index(name = "idx_app_banners_type", columnList = "banner_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional tag badge (e.g., "WHOLESALE MEGA MELA")
    @Column(length = 80)
    private String tag;

    // Main heading (optional if pure image banner)
    @Column(length = 150)
    private String title;

    // Subtitle / offer line
    @Column(length = 300)
    private String subtitle;

    // CTA button label (default: "Explore Lots →")
    @Column(name = "button_text", length = 60)
    private String buttonText;

    // Optional Cloudinary HTTPS URL (if null/blank, app uses gradient card style)
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // HOME_CAROUSEL or PROMO_POPUP (future-ready)
    @Column(name = "banner_type", nullable = false, length = 30)
    @Builder.Default
    private String bannerType = "HOME_CAROUSEL";

    // ALL_PRODUCTS, CATEGORY, SEARCH, PRODUCT, NONE
    @Column(name = "action_type", nullable = false, length = 30)
    @Builder.Default
    private String actionType = "ALL_PRODUCTS";

    // Category name, search keyword, or product ID based on actionType
    @Column(name = "action_value", length = 250)
    private String actionValue;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.bannerType == null || this.bannerType.isBlank()) this.bannerType = "HOME_CAROUSEL";
        if (this.actionType == null || this.actionType.isBlank()) this.actionType = "ALL_PRODUCTS";
        if (this.displayOrder == null) this.displayOrder = 0;
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}