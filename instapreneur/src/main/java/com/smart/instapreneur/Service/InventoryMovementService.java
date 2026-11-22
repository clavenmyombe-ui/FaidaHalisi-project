package com.smart.instapreneur.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smart.instapreneur.Model.InventoryMovement;
import com.smart.instapreneur.Model.Products;
import com.smart.instapreneur.Repository.InventoryMovementRepository;

/** Huhifadhi mabadiliko yote ya stock sehemu moja ili rekodi ziwe na muundo unaofanana. */
@Service
public class InventoryMovementService {
    private final InventoryMovementRepository repository;
    public InventoryMovementService(InventoryMovementRepository repository) { this.repository = repository; }

    /** Njia fupi ya zamani: idadi = thamani kamili ya badiliko, bila gharama wala kumbukumbu. */
    @Transactional
    public InventoryMovement record(Products product, String type, int delta, int before, int after, String reason, String actor) {
        return record(product, type, delta, before, after, Math.abs(delta), null, null, reason, actor);
    }

    /**
     * @param delta    badiliko la stock INAYOPATIKANA (chanya = inaongezeka)
     * @param quantity vipande vilivyohusika (daima chanya), hata kama delta ni 0 (mfano ORDER_ISSUED)
     * @param unitCost gharama ya kipande wakati huo, kwa kukokotoa hasara
     * @param reference namba ya oda / mapokezi inayohusiana
     */
    @Transactional
    public InventoryMovement record(Products product, String type, int delta, int before, int after,
            int quantity, BigDecimal unitCost, String reference, String reason, String actor) {
        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantityChange(delta);
        movement.setQuantityBefore(before);
        movement.setQuantityAfter(after);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setReference(reference);
        movement.setReason(reason);
        movement.setActorEmail(actor == null || actor.isBlank() ? "system" : actor);
        movement.setCreatedAt(LocalDateTime.now());
        return repository.save(movement);
    }
}
