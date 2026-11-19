package com.retail.flow.banner.service;

import com.retail.flow.banner.dto.BannerRequestDto;
import com.retail.flow.banner.dto.BannerResponseDto;
import com.retail.flow.banner.entity.AppBanner;
import com.retail.flow.banner.repository.AppBannerRepository;
import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private static final Set<String> ALLOWED_BANNER_TYPES = Set.of("HOME_CAROUSEL", "PROMO_POPUP");
    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of("ALL_PRODUCTS", "CATEGORY", "SEARCH", "PRODUCT", "NONE");

    private final AppBannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public List<BannerResponseDto> getActiveBanners(String bannerType) {
        String normalizedType = normalizeBannerType(bannerType);
        return bannerRepository.findByBannerTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(normalizedType)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllBannersForAdmin() {
        return bannerRepository.findAllByOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BannerResponseDto createBanner(BannerRequestDto dto) {
        validateBannerContent(dto);
        AppBanner banner = AppBanner.builder()
                .tag(cleanNullable(dto.getTag()))
                .title(cleanNullable(dto.getTitle()))
                .subtitle(cleanNullable(dto.getSubtitle()))
                .buttonText(cleanNullable(dto.getButtonText()))
                .imageUrl(cleanNullable(dto.getImageUrl()))
                .bannerType(normalizeBannerType(dto.getBannerType()))
                .actionType(normalizeActionType(dto.getActionType()))
                .actionValue(cleanNullable(dto.getActionValue()))
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        return mapToDto(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponseDto updateBanner(Long id, BannerRequestDto dto) {
        validateBannerContent(dto);
        AppBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));

        banner.setTag(cleanNullable(dto.getTag()));
        banner.setTitle(cleanNullable(dto.getTitle()));
        banner.setSubtitle(cleanNullable(dto.getSubtitle()));
        banner.setButtonText(cleanNullable(dto.getButtonText()));
        banner.setImageUrl(cleanNullable(dto.getImageUrl()));
        banner.setBannerType(normalizeBannerType(dto.getBannerType()));
        banner.setActionType(normalizeActionType(dto.getActionType()));
        banner.setActionValue(cleanNullable(dto.getActionValue()));
        banner.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : banner.getDisplayOrder());
        if (dto.getActive() != null) {
            banner.setActive(dto.getActive());
        }

        return mapToDto(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponseDto updateBannerStatus(Long id, boolean active) {
        AppBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));
        banner.setActive(active);
        return mapToDto(bannerRepository.save(banner));
    }

    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Banner not found with id: " + id);
        }
        bannerRepository.deleteById(id);
    }

    private void validateBannerContent(BannerRequestDto dto) {
        if (!StringUtils.hasText(dto.getTitle()) && !StringUtils.hasText(dto.getImageUrl())) {
            throw new BusinessValidationException("Provide at least a banner Title or an Image URL.");
        }
        String actionType = normalizeActionType(dto.getActionType());
        if (("CATEGORY".equals(actionType) || "SEARCH".equals(actionType) || "PRODUCT".equals(actionType))
                && !StringUtils.hasText(dto.getActionValue())) {
            throw new BusinessValidationException("Action value is required when actionType is " + actionType + ".");
        }
    }

    private String normalizeBannerType(String raw) {
        String normalized = StringUtils.hasText(raw) ? raw.trim().toUpperCase() : "HOME_CAROUSEL";
        if (!ALLOWED_BANNER_TYPES.contains(normalized)) {
            throw new BusinessValidationException("Invalid bannerType. Allowed: " + ALLOWED_BANNER_TYPES);
        }
        return normalized;
    }

    private String normalizeActionType(String raw) {
        String normalized = StringUtils.hasText(raw) ? raw.trim().toUpperCase() : "ALL_PRODUCTS";
        if (!ALLOWED_ACTION_TYPES.contains(normalized)) {
            throw new BusinessValidationException("Invalid actionType. Allowed: " + ALLOWED_ACTION_TYPES);
        }
        return normalized;
    }

    private String cleanNullable(String val) {
        return StringUtils.hasText(val) ? val.trim() : null;
    }

    private BannerResponseDto mapToDto(AppBanner banner) {
        return BannerResponseDto.builder()
                .id(banner.getId())
                .tag(banner.getTag())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .buttonText(banner.getButtonText())
                .imageUrl(banner.getImageUrl())
                .bannerType(banner.getBannerType())
                .actionType(banner.getActionType())
                .actionValue(banner.getActionValue())
                .displayOrder(banner.getDisplayOrder())
                .active(banner.getActive())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}