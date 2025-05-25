package com.example.IntisoftTest.Security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.IntisoftTest.model.Role;
import com.example.IntisoftTest.model.User;
import com.example.IntisoftTest.repository.UserRepository;

@Service
public class AccessControlService {

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean hasRole(Role role) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() == role;
    }

    public boolean hasAnyRole(Role... roles) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        return Arrays.stream(roles).anyMatch(r -> currentUser.getRole() == r);
    }

    public boolean isOwner(Long ownerId) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getId().equals(ownerId);
    }

    public boolean isOwnerOrHasRole(Long ownerId, Role role) {
        return isOwner(ownerId) || hasRole(role);
    }
}
