package com.retail.flow.banner.controller;

import com.retail.flow.banner.dto.BannerRequestDto;
import com.retail.flow.banner.dto.BannerResponseDto;
import com.retail.flow.banner.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    // 1. PUBLIC / BUYER APP: Active banners ordered by displayOrder
    @GetMapping("/banners/active")
    public ResponseEntity<List<BannerResponseDto>> getActiveBanners(
            @RequestParam(defaultValue = "HOME_CAROUSEL") String type) {
        return ResponseEntity.ok(bannerService.getActiveBanners(type));
    }

    // 2. ADMIN ONLY (protected by /api/v1/admin/** in SecurityConfig)
    @GetMapping("/admin/banners")
    public ResponseEntity<List<BannerResponseDto>> getAllBannersForAdmin() {
        return ResponseEntity.ok(bannerService.getAllBannersForAdmin());
    }

    @PostMapping("/admin/banners")
    public ResponseEntity<BannerResponseDto> createBanner(@Valid @RequestBody BannerRequestDto requestDto) {
        return new ResponseEntity<>(bannerService.createBanner(requestDto), HttpStatus.CREATED);
    }

    @PutMapping("/admin/banners/{id}")
    public ResponseEntity<BannerResponseDto> updateBanner(
            @PathVariable Long id,
            @Valid @RequestBody BannerRequestDto requestDto) {
        return ResponseEntity.ok(bannerService.updateBanner(id, requestDto));
    }

    @PatchMapping("/admin/banners/{id}/status")
    public ResponseEntity<BannerResponseDto> updateBannerStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(bannerService.updateBannerStatus(id, active));
    }

    @DeleteMapping("/admin/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}