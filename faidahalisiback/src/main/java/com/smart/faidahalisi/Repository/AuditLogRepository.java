package com.smart.faidahalisi.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.faidahalisi.Model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByStoreIdOrderByCreatedAtDesc(Long storeId);
}
