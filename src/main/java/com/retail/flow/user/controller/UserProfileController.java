package com.retail.flow.user.controller;

import com.retail.flow.common.security.CurrentUserService;
import com.retail.flow.user.dto.UserProfileDto;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserProfileDto> getMyProfile(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(mapToProfileDto(user));
    }

    @PutMapping("/me/profile")
    @Transactional
    public ResponseEntity<UserProfileDto> updateMyProfile(
            @Valid @RequestBody UserProfileDto requestDto,
            Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication.getName());
        User managedUser = userRepository.findById(currentUser.getId()).orElseThrow();

        if (StringUtils.hasText(requestDto.getName())) {
            managedUser.setName(requestDto.getName().trim());
        }
        managedUser.setBusinessName(StringUtils.hasText(requestDto.getBusinessName())
                ? requestDto.getBusinessName().trim() : null);
        managedUser.setGstNumber(StringUtils.hasText(requestDto.getGstNumber())
                ? requestDto.getGstNumber().trim().toUpperCase() : null);
        managedUser.setBusinessPhone(StringUtils.hasText(requestDto.getBusinessPhone())
                ? requestDto.getBusinessPhone().trim() : null);
        managedUser.setBusinessAddress(StringUtils.hasText(requestDto.getBusinessAddress())
                ? requestDto.getBusinessAddress().trim() : null);

        User saved = userRepository.save(managedUser);
        return ResponseEntity.ok(mapToProfileDto(saved));
    }

    private UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .businessName(user.getBusinessName())
                .gstNumber(user.getGstNumber())
                .businessPhone(user.getBusinessPhone())
                .businessAddress(user.getBusinessAddress())
                .build();
    }
}