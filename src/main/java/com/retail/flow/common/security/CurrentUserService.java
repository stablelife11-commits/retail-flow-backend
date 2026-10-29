package com.retail.flow.common.security;

import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser(String fallbackEmail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            User cachedUser = principal.getUser();
            if (fallbackEmail == null || cachedUser.getEmail().equalsIgnoreCase(fallbackEmail)) {
                return cachedUser;
            }
        }
        return userRepository.findByEmail(fallbackEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}