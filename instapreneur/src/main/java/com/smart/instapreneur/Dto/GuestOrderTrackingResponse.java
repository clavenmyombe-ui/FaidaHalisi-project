package com.smart.instapreneur.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Taarifa chache tu zinazohitajika na mnunuzi kufuatilia oda yake. Hakuna anwani kamili,
 * stock, gharama wala taarifa za wateja wengine.
 */
public record GuestOrderTrackingResponse(
        String orderNumber,
        String status,
        String statusLabel,
        BigDecimal totalAmount,
        String paymentState,          // UNPAID | PARTIAL | PAID (malipo yaliyohakikiwa)
        LocalDateTime createdAt,
        List<Item> items,
        boolean trackingActive,
        boolean availabilityNeedsConfirmation,
        String whatsappNumber,        // kwa msaada; null ikiwa duka halijaiweka
        String storeName,
        String message) {
    public record Item(String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
