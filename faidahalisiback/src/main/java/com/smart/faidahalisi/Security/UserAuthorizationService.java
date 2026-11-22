package com.smart.faidahalisi.Security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.smart.faidahalisi.Repository.UserRepository;

@Component("userAuthorization")
public class UserAuthorizationService {
    private final UserRepository userRepository;
    public UserAuthorizationService(UserRepository userRepository) { this.userRepository = userRepository; }
    public boolean isSelf(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return userRepository.findById(userId)
                .map(user -> user.getEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }
}
