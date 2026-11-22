package com.smart.faidahalisi.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO ya historia ya stock; hairudishi entity za ndani za duka au mtumiaji. */
public record InventoryMovementResponse(
        Long id, Long productId, String productName, String movementType,
        int quantityChange, int quantityBefore, int quantityAfter,
        int quantity, BigDecimal unitCost, String reference,
        String reason, String actorEmail, LocalDateTime createdAt) {
}
