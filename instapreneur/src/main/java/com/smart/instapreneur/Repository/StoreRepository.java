package com.smart.instapreneur.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.instapreneur.Model.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
    // Custom query methods
    Optional<Store> findBySlug(String slug);
    
}
