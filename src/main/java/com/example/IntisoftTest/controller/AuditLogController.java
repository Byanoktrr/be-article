package com.example.IntisoftTest.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.IntisoftTest.model.AuditLog;
import com.example.IntisoftTest.repository.AuditLogRepository;
import com.example.IntisoftTest.util.JwtUtil;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLogs(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("status", HttpStatus.UNAUTHORIZED.value());
                response.put("message", "Missing or invalid Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                response.put("status", HttpStatus.UNAUTHORIZED.value());
                response.put("message", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String role = jwtUtil.extractRole(token);
            if (!"SUPER_ADMIN".equals(role)) {
                response.put("status", HttpStatus.FORBIDDEN.value());
                response.put("message", "Access denied: Requires SUPER_ADMIN role");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            List<AuditLog> logs = auditLogRepository.findAll();
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Success");
            response.put("data", logs);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
