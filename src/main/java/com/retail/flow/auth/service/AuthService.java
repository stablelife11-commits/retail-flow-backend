package com.retail.flow.auth.service;

import com.retail.flow.auth.dto.AuthResponseDto;
import com.retail.flow.auth.dto.LoginRequestDto;
import com.retail.flow.auth.dto.RegisterRequestDto;
import com.retail.flow.common.security.JwtTokenProvider;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository; // 🟢 NAYA: Customer Repository add kiya gaya
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RuntimeException("Email is already registered!");
        }

        User.Role assignedRole = User.Role.BUYER;
        if (requestDto.getRole() != null && requestDto.getRole().equalsIgnoreCase("SELLER")) {
            assignedRole = User.Role.SELLER;
        }

        User user = User.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(assignedRole)
                .active(true)
                .build();

        user = userRepository.save(user);

        // 🟢 NAYA: Register hote hi Customer profile auto-create hogi
        String safeMobile = String.format("99%08d", user.getId()); // Unique dummy mobile generate karega
        Customer customer = Customer.builder()
                .name(user.getName())
                .email(user.getEmail())
                .mobile(safeMobile)
                .active(true)
                .build();
        customer = customerRepository.save(customer);

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponseDto.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .customerId(customer.getId()) // 🟢 Send true customer ID
                .build();
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password!"));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password!");
        }

        // 🟢 NAYA: Login ke waqt check karega, agar Customer profile nahi hai toh bana dega
        Customer customer = customerRepository.findByEmail(user.getEmail())
                .orElseGet(() -> {
                    String safeMobile = String.format("99%08d", user.getId());
                    Customer newCust = Customer.builder()
                            .name(user.getName())
                            .email(user.getEmail())
                            .mobile(safeMobile)
                            .active(true)
                            .build();
                    return customerRepository.save(newCust);
                });

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponseDto.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .customerId(customer.getId()) // 🟢 Send true customer ID
                .build();
    }
}