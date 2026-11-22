package com.smart.faidahalisi.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.faidahalisi.Model.IncomingStock;

public interface IncomingStockRepository extends JpaRepository<IncomingStock, Long> {
    boolean existsByProductId(Long productId);

    List<IncomingStock> findByProductStoreIdOrderByCreatedAtDesc(Long storeId);
    List<IncomingStock> findByProductIdAndStatus(Long productId, String status);
}
