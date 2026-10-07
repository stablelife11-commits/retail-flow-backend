package com.retail.flow.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll() // Public endpoints

                        // 🟢 FIX: STRICT ROLE-BASED ACCESS CONTROL (RBAC)
                        // Sirf SELLER ya ADMIN hi Products add/update kar sakte hain
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")

                        // Sirf SELLER/ADMIN in backend management APIs ko dekh/use kar sakte hain
                        .requestMatchers("/api/v1/sales/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/purchases/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/reports/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/returns/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/suppliers/**").hasAnyRole("SELLER", "ADMIN")

                        // Baaki sabhi requests (jaise GET products, GET/POST orders) koi bhi logged-in user kar sakta hai
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}