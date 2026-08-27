package com.stockpilot.service;

import com.stockpilot.entity.User;

public interface AuditLogService {
    void logAction(User user, String action, String entityType, Long entityId, String description);
}
