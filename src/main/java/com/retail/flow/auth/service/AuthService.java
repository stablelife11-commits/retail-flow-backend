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

        if (customerRepository.existsBySellerIdIsNullAndMobile(trimmedMobile)) {
            throw new BusinessValidationException("Mobile number is already registered to another buyer account!");
        }

        User user = User.builder()
                .name(requestDto.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(User.Role.BUYER)
                .active(true)
                .build();

        user = userRepository.save(user);

        Customer customer = getOrCreatePlatformBuyerProfile(user, trimmedMobile);

        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .customerId(customer.getId())
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
                .build();
    }

    // SECURITY FIX (F-002): Never overwrite or hijack seller-managed offline Customer records.
    // Always maintain an isolated platform buyer Customer record (sellerId = null).
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