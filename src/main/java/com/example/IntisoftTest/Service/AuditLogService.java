package com.example.IntisoftTest.Service;

import com.example.IntisoftTest.model.AuditLog;
import com.example.IntisoftTest.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(Long userId, String action, String description, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDescription(description);
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress(ipAddress);

        auditLogRepository.save(log);
    }
}
