package com.retail.flow.common.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
                .cors(cors -> cors.configure(http))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication token is missing, invalid, or expired.\"}"
                            );
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Admin User & Seller Management
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Product Catalog
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")

                        // Order Endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/my-orders").hasAnyRole("BUYER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders", "/api/v1/orders/checkout").hasAnyRole("BUYER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/seller/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status").hasAnyRole("BUYER", "SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("ADMIN")

                        // Back-Office ERP Modules
                        .requestMatchers("/api/v1/customers/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/expenses/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/payments/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/sales/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/purchases/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/reports/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/returns/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers("/api/v1/suppliers/**").hasAnyRole("SELLER", "ADMIN")

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}