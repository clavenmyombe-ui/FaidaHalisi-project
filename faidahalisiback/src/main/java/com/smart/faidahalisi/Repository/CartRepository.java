package com.smart.faidahalisi.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.faidahalisi.Model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerId(Long customerId);
    Optional<Cart> findByGuestKey(String guestKey);
}
