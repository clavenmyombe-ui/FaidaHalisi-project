package com.smart.instapreneur.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.instapreneur.Model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Custom query methods
    Optional<Payment> findByTransactionalRefference(String transactionalRefference);
    boolean existsByTransactionalRefference(String transactionalRefference);

    List<Payment> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    List<Payment> findByOrderStoreIdAndVerificationStatus(Long storeId, String verificationStatus);
    List<Payment> findByOrderStoreIdAndVerificationStatusAndVerifiedAtBetween(
            Long storeId, String verificationStatus, LocalDateTime from, LocalDateTime to);
}
