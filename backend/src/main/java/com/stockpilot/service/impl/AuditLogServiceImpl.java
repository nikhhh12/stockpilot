package com.stockpilot.service.impl;

import com.stockpilot.entity.AuditLog;
import com.stockpilot.entity.User;
import com.stockpilot.repository.AuditLogRepository;
import com.stockpilot.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logAction(User user, String action, String entityType, Long entityId, String description) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        auditLogRepository.save(log);
    }
}
