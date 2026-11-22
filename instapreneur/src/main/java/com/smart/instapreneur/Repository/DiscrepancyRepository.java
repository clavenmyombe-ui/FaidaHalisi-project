package com.smart.instapreneur.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.instapreneur.Model.Discrepancy;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long> {
    List<Discrepancy> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Discrepancy> findByStoreIdAndStatus(Long storeId, String status);
}
