package com.smart.faidahalisi.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.faidahalisi.Model.Discrepancy;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long> {
    List<Discrepancy> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Discrepancy> findByStoreIdAndStatus(Long storeId, String status);
}
