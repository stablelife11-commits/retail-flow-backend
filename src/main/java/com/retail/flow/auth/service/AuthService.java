package com.retail.flow.auth.service;

import com.retail.flow.auth.dto.AuthResponseDto;
import com.retail.flow.auth.dto.LoginRequestDto;
import com.retail.flow.auth.dto.RegisterRequestDto;
import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.security.JwtTokenProvider;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto requestDto) {
        String normalizedEmail = requestDto.getEmail().trim().toLowerCase();
        String trimmedMobile = requestDto.getMobile().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessValidationException("Email is already registered!");
        }

        User.Role targetRole = User.Role.BUYER;
        if (StringUtils.hasText(requestDto.getRole())) {
            String requestedRole = requestDto.getRole().trim().toUpperCase();
            if ("SELLER".equals(requestedRole)) {
                targetRole = User.Role.SELLER;
            } else if (!"BUYER".equals(requestedRole)) {
                throw new BusinessValidationException("Invalid registration role. Only BUYER or SELLER registration is allowed.");
            }
        }

        if (targetRole == User.Role.BUYER && customerRepository.existsBySellerIdIsNullAndMobile(trimmedMobile)) {
            throw new BusinessValidationException("Mobile number is already registered to another buyer account!");
        }

        User user = User.builder()
                .name(requestDto.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(targetRole)
                .active(true)
                .businessName(StringUtils.hasText(requestDto.getBusinessName()) ? requestDto.getBusinessName().trim() : null)
                .gstNumber(StringUtils.hasText(requestDto.getGstNumber()) ? requestDto.getGstNumber().trim().toUpperCase() : null)
                .businessPhone(trimmedMobile)
                .businessAddress(StringUtils.hasText(requestDto.getBusinessAddress()) ? requestDto.getBusinessAddress().trim() : null)
                .build();

        user = userRepository.save(user);

        Long customerId = null;
        if (targetRole == User.Role.BUYER) {
            Customer customer = getOrCreatePlatformBuyerProfile(user, trimmedMobile);
            customerId = customer.getId();
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .customerId(customerId)
                .businessName(user.getBusinessName())
                .gstNumber(user.getGstNumber())
                .businessPhone(user.getBusinessPhone())
                .businessAddress(user.getBusinessAddress())
                .build();
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto requestDto) {
        String normalizedEmail = requestDto.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessValidationException("Invalid email or password!"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessValidationException("Your account has been deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new BusinessValidationException("Invalid email or password!");
        }

        Long customerId = null;
        if (user.getRole() == User.Role.BUYER) {
            Customer customer = getOrCreatePlatformBuyerProfile(user, null);
            customerId = customer.getId();
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .customerId(customerId)
                .businessName(user.getBusinessName())
                .gstNumber(user.getGstNumber())
                .businessPhone(user.getBusinessPhone())
                .businessAddress(user.getBusinessAddress())
                .build();
    }

    private Customer getOrCreatePlatformBuyerProfile(User user, String providedMobile) {
        return customerRepository.findByEmail(user.getEmail())
                .orElseGet(() -> {
                    String fallbackMobile = StringUtils.hasText(providedMobile)
                            ? providedMobile
                            : String.format("00%08d", user.getId());

                    Customer newCust = Customer.builder()
                            .sellerId(null)
                            .name(user.getName())
                            .email(user.getEmail())
                            .mobile(fallbackMobile)
                            .active(true)
                            .build();
                    return customerRepository.save(newCust);
                });
    }
}