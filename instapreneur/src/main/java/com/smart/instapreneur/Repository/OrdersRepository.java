package com.smart.instapreneur.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.smart.instapreneur.Model.Orders;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    // Custom query methods
    Optional<Orders> findByOrderNumber(String orderNumber);
    Optional<Orders> findByTrackingToken(String trackingToken);
    List<Orders> findByCustomerId(Long customerId);
    List<Orders> findByStoreId(Long storeId);
    Optional<Orders> findByPaymentId(Long paymentId);
    List<Orders> findByCustomerUserEmailIgnoreCase(String email);

    /**
     * Inafunga mstari wa oda wakati wa kubadilisha hali yake. Bila lock, maombi mawili ya
     * "idhinisha" kwa wakati mmoja yangeweza kupita ukaguzi wa hali yote mawili na kupunguza stock mara mbili.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.id = :id")
    Optional<Orders> lockById(@Param("id") Long id);

    List<Orders> findByStoreIdAndStatusIn(Long storeId, Collection<String> statuses);
    List<Orders> findByStoreIdAndDeliveredAtBetween(Long storeId, LocalDateTime from, LocalDateTime to);
    List<Orders> findByStoreIdAndCancelledAfterReservationTrue(Long storeId);
}
