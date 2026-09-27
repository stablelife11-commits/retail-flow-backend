package com.retail.flow.common.config;

import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if any ADMIN already exists
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() == User.Role.ADMIN);

        if (!adminExists) {
            User admin = User.builder()
                    .name("rohit")
                    .email("admin@retailflow.com")
                    .password(passwordEncoder.encode("admin123")) // Encrypted password
                    .role(User.Role.ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);
            System.out.println(">>> Default Admin Created Successfully: admin@retailflow.com / admin123 <<<");
        }
    }
}