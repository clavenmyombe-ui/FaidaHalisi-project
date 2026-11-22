package com.smart.instapreneur.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Model.AuditLog;
import com.smart.instapreneur.Repository.AuditLogRepository;

/**
 * Inaandika historia ya vitendo muhimu. Inaandika ndani ya transaction ile ile ya kitendo:
 * kitendo kikishindwa, rekodi yake pia haibaki (na kikifanikiwa, rekodi lazima ibaki).
 */
@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(Long storeId, String actor, String action, String entityType, Object entityId,
            String oldValue, String newValue, String reason) {
        AuditLog log = new AuditLog();
        log.setStoreId(storeId);
        log.setActorEmail(actor == null || actor.isBlank() ? "system" : actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId == null ? null : String.valueOf(entityId));
        log.setOldValue(cut(oldValue, 500));
        log.setNewValue(cut(newValue, 500));
        log.setReason(cut(reason, 255));
        log.setCreatedAt(LocalDateTime.now());
        repository.save(log);
    }

    private String cut(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
