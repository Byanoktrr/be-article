package com.example.IntisoftTest.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.IntisoftTest.Security.AccessControlService;
import com.example.IntisoftTest.Service.AuditLogService;
import com.example.IntisoftTest.model.Role;
import com.example.IntisoftTest.model.User;
import com.example.IntisoftTest.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AccessControlService accessControlService;

    private Map<String, Object> response(boolean success, String message, Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", success);
        res.put("message", message);
        if (data != null) res.put("data", data);
        return res;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        if (!accessControlService.hasRole(Role.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        return ResponseEntity.ok(response(true, "All users retrieved", userRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        if (!accessControlService.hasRole(Role.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        Optional<User> userOpt = userRepository.findById(id);
        return userOpt
            .map(user -> ResponseEntity.ok(response(true, "User found", user)))
            .orElse(ResponseEntity.status(404).body(response(false, "User not found", null)));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user, HttpServletRequest request) {
        if (!accessControlService.hasRole(Role.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getRole() == null) {
            user.setRole(Role.VIEWER);
        }

        User savedUser = userRepository.save(user);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(savedUser.getId(), "USER_CREATE", "Created new user by " + actor, getClientIp(request));

        return ResponseEntity.ok(response(true, "User created successfully", savedUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser, HttpServletRequest request) {
        if (!accessControlService.hasRole(Role.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(response(false, "User not found", null));
        }

        User user = userOpt.get();
        user.setFullname(updatedUser.getFullname());
        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        user.setPassword(updatedUser.getPassword());
        user.setRole(updatedUser.getRole());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(user.getId(), "USER_UPDATE", "Updated user data by " + actor, getClientIp(request));

        return ResponseEntity.ok(response(true, "User updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        if (!accessControlService.hasRole(Role.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(response(false, "User not found", null));
        }

        userRepository.deleteById(id);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(id, "USER_DELETE", "Deleted user by " + actor, getClientIp(request));

        return ResponseEntity.ok(response(true, "User deleted successfully", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return xfHeader == null ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }
}
