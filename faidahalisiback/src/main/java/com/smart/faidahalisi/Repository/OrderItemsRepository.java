package com.smart.faidahalisi.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.faidahalisi.Model.OrderItems;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
    // Custom query methods
    Optional<OrderItems> findByProductId(Long productId);

    List<OrderItems> findByOrderId(Long orderId);

    boolean existsByProductId(Long productId);

    /** Vipande vya bidhaa hii vilivyoshikiliwa sasa na oda zilizoidhinishwa (bado hazijatoka ghalani). */
    @Query("select coalesce(sum(i.quantity), 0) from OrderItems i where i.product.id = :productId and i.order.stockReserved = true")
    long reservedUnits(@Param("productId") Long productId);
}
