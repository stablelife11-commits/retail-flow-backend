package com.retail.flow.banner.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BannerRequestDto {

    @Size(max = 80, message = "Tag cannot exceed 80 characters")
    private String tag;

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 300, message = "Subtitle cannot exceed 300 characters")
    private String subtitle;

    @Size(max = 60, message = "Button text cannot exceed 60 characters")
    private String buttonText;

    @Size(max = 1000, message = "Image URL cannot exceed 1000 characters")
    private String imageUrl;

    // HOME_CAROUSEL (default) or PROMO_POPUP
    private String bannerType = "HOME_CAROUSEL";

    // ALL_PRODUCTS (default), CATEGORY, SEARCH, PRODUCT, NONE
    private String actionType = "ALL_PRODUCTS";

    @Size(max = 250, message = "Action value cannot exceed 250 characters")
    private String actionValue;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder = 0;

    private Boolean active = true;
}