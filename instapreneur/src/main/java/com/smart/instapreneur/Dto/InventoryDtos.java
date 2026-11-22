package com.smart.instapreneur.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO za stock: zinaonyeshwa kwa mmiliki tu (zina gharama na idadi halisi). */
public final class InventoryDtos {
    private InventoryDtos() {}

    /**
     * onHand = stock halisi ghalani (available + reserved).
     * available = inayoweza kuahidiwa kwa oda mpya. reserved = imeshikiliwa na oda zilizoidhinishwa.
     * incoming = inatarajiwa kufika (haijapokelewa, haiko kwenye onHand).
     */
    public record StockRow(Long productId, String name, String status, int onHand, int available,
            long reserved, long incoming, BigDecimal costPrice, BigDecimal stockValue) {}

    public record CountResult(int expected, int counted, int difference, Long discrepancyId, String message) {}

    public record IncomingResponse(Long id, Long productId, String productName, int expectedQuantity,
            int receivedQuantity, BigDecimal unitCost, String supplier, String status, String note,
            String createdBy, LocalDateTime createdAt, String receivedBy, LocalDateTime receivedAt) {}
}
