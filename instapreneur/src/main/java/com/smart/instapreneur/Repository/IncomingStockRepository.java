package com.smart.instapreneur.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.instapreneur.Model.IncomingStock;

public interface IncomingStockRepository extends JpaRepository<IncomingStock, Long> {
    boolean existsByProductId(Long productId);

    List<IncomingStock> findByProductStoreIdOrderByCreatedAtDesc(Long storeId);
    List<IncomingStock> findByProductIdAndStatus(Long productId, String status);
}
