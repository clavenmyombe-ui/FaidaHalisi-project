package com.smart.faidahalisi.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.faidahalisi.Model.InventoryMovement;

/** Hupata historia ya stock ya bidhaa moja kwa mpangilio wa hivi karibuni kwanza. */
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    boolean existsByProductId(Long productId);

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryMovement> findTop200ByProductStoreIdOrderByCreatedAtDesc(Long storeId);

    List<InventoryMovement> findByProductStoreIdAndMovementTypeInAndCreatedAtBetween(
            Long storeId, Collection<String> types, LocalDateTime from, LocalDateTime to);
}
