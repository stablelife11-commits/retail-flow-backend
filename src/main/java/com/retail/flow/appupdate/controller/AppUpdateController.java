package com.retail.flow.appupdate.controller;

import com.retail.flow.appupdate.dto.AppUpdateDto;
import com.retail.flow.appupdate.entity.AppVersionConfig;
import com.retail.flow.appupdate.repository.AppVersionConfigRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/app-update")
@RequiredArgsConstructor
public class AppUpdateController {

    private final AppVersionConfigRepository repository;

    // 1. Public Endpoint: Buyer & Seller Apps इसे कॉल करके अपडेट चेक करेंगी
    @GetMapping("/check")
    @Transactional(readOnly = true)
    public ResponseEntity<AppUpdateDto> checkUpdate(@RequestParam(defaultValue = "BUYER") String appType) {
        String normalizedType = appType.trim().toUpperCase();

        AppVersionConfig config = repository.findById(normalizedType)
                .orElseGet(() -> AppVersionConfig.builder()
                        .appType(normalizedType)
                        .latestVersionCode(1)
                        .versionName("1.0")
                        .apkUrl("https://github.com/stablelife11-commits/customershoppingapp/releases/download/v1.0/CustomerApp.apk")
                        .releaseNotes("Initial Release")
                        .forceUpdate(false)
                        .build());

        return ResponseEntity.ok(mapToDto(config));
    }

    // 2. Protected Endpoint (SELLER / ADMIN): बिना सर्वर री-डिप्लॉय किए नया अपडेट पुश करने के लिए
    @PutMapping
    @Transactional
    public ResponseEntity<AppUpdateDto> publishNewUpdate(@Valid @RequestBody AppUpdateDto requestDto) {
        String normalizedType = requestDto.getAppType().trim().toUpperCase();

        AppVersionConfig config = repository.findById(normalizedType)
                .orElseGet(() -> AppVersionConfig.builder().appType(normalizedType).build());

        config.setLatestVersionCode(requestDto.getLatestVersionCode());
        config.setVersionName(requestDto.getVersionName().trim());
        config.setApkUrl(requestDto.getApkUrl().trim());
        config.setReleaseNotes(requestDto.getReleaseNotes());
        config.setForceUpdate(requestDto.isForceUpdate());

        AppVersionConfig saved = repository.save(config);
        return ResponseEntity.ok(mapToDto(saved));
    }

    private AppUpdateDto mapToDto(AppVersionConfig config) {
        return AppUpdateDto.builder()
                .appType(config.getAppType())
                .latestVersionCode(config.getLatestVersionCode())
                .versionName(config.getVersionName())
                .apkUrl(config.getApkUrl())
                .releaseNotes(config.getReleaseNotes())
                .forceUpdate(Boolean.TRUE.equals(config.getForceUpdate()))
                .build();
    }
}