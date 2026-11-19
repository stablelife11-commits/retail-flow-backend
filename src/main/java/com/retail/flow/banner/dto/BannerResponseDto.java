package com.retail.flow.banner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BannerResponseDto {
    private Long id;
    private String tag;
    private String title;
    private String subtitle;
    private String buttonText;
    private String imageUrl;
    private String bannerType;
    private String actionType;
    private String actionValue;
    private Integer displayOrder;
    private Boolean active;
    private LocalDateTime updatedAt;
}