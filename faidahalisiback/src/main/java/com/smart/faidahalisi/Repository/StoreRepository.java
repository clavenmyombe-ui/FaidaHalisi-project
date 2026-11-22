package com.smart.faidahalisi.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.faidahalisi.Model.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
    // Custom query methods
    Optional<Store> findBySlug(String slug);
    
}
