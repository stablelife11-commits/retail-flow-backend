package com.retail.flow.user.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.user.dto.UserResponseDto;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDto updateUserRole(Long userId, String roleStr, String adminEmail) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (targetUser.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new BusinessValidationException("Administrators cannot modify their own role.");
        }

        User.Role newRole;
        try {
            newRole = User.Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Invalid role: " + roleStr
                    + ". Allowed values: " + Arrays.toString(User.Role.values()));
        }

        targetUser.setRole(newRole);
        return mapToDto(userRepository.save(targetUser));
    }

    @Transactional
    public UserResponseDto updateUserStatus(Long userId, boolean active, String adminEmail) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (targetUser.getEmail().equalsIgnoreCase(adminEmail) && !active) {
            throw new BusinessValidationException("Administrators cannot deactivate their own account.");
        }

        targetUser.setActive(active);
        return mapToDto(userRepository.save(targetUser));
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.getActive())
                .build();
    }
}