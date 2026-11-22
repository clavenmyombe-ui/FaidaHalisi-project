package com.smart.instapreneur.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.instapreneur.Model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByStoreIdOrderByCreatedAtDesc(Long storeId);
}
