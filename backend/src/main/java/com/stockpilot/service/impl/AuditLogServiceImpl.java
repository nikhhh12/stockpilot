package com.stockpilot.service.impl;

import com.stockpilot.dto.response.AuditLogResponse;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.entity.AuditLog;
import com.stockpilot.entity.User;
import com.stockpilot.mapper.AuditLogMapper;
import com.stockpilot.repository.AuditLogRepository;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.security.SecurityUtils;
import com.stockpilot.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final UserRepository userRepository;

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

    @Override
    @Transactional
    public void logAction(String action, String entityType, Long entityId, String description) {
        User user = SecurityUtils.getCurrentUserEmail()
                .flatMap(userRepository::findByEmail)
                .orElse(null);
        logAction(user, action, entityType, entityId, description);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(String action, String entityType, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll((root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(action)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(entityType)) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("entityType")), entityType.toLowerCase()));
            }
            return predicate;
        }, pageable);

        return PagedResponse.from(page.map(auditLogMapper::toResponse));
    }
}
