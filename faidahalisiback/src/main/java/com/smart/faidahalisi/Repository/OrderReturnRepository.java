package com.smart.faidahalisi.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.faidahalisi.Model.OrderReturn;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {
    boolean existsByProductId(Long productId);

    List<OrderReturn> findByOrderId(Long orderId);
    List<OrderReturn> findByOrderStoreIdAndCreatedAtBetween(Long storeId, LocalDateTime from, LocalDateTime to);
}
