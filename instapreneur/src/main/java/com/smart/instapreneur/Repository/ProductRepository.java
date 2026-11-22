package com.smart.instapreneur.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.instapreneur.Model.Products;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Products, Long> {
    // Custom query methods
    Optional<Products> findByName(String name);
    List<Products> findByStoreId(Long storeId);
    List<Products> findByStoreIdAndStatus(Long storeId, String status);

    /**
     * Inafunga (lock) mistari ya bidhaa kwenye database mpaka transaction iishe.
     * Kwa nini: oda mbili zikiidhinishwa kwa wakati mmoja zingeweza kusoma stock ile ile (5) na
     * kila moja kuchukua 5 -> stock ingekuwa -5. Kwa lock, ya pili inasubiri ya kwanza imalize
     * kisha inaona stock halisi. "order by id" inahakikisha kila mtu anafunga kwa mpangilio mmoja
     * (inazuia deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Products p where p.id in :ids order by p.id")
    List<Products> lockByIds(@Param("ids") Collection<Long> ids);
}
