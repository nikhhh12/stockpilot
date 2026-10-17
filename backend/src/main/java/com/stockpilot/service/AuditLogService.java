package com.stockpilot.service;

import com.stockpilot.dto.response.AuditLogResponse;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.entity.User;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void logAction(User user, String action, String entityType, Long entityId, String description);
    void logAction(String action, String entityType, Long entityId, String description);
    PagedResponse<AuditLogResponse> getAuditLogs(String action, String entityType, Pageable pageable);
}
