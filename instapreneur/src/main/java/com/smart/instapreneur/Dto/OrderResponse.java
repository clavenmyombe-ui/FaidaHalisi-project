package com.smart.instapreneur.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Muonekano wa oda kwa mmiliki au mteja mwenyewe. Hauna gharama ya ununuzi, stock halisi wala
 * taarifa za muuzaji. (Entity ya Orders hairudishwi moja kwa moja tena kwa sababu ya hatari ya kuvujisha data.)
 */
public record OrderResponse(
        Long id,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        BigDecimal returnsCredit,
        BigDecimal paidAmount,      // malipo YALIYOHAKIKIWA tu
        BigDecimal balance,         // deni linalobaki = jumla - returns - malipo yaliyohakikiwa
        String paymentState,        // UNPAID | PARTIAL | PAID
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        String deliveryAddress,
        Long storeId,
        boolean stockReserved,
        boolean needsStockReview,
        String cancelReason,
        CustomerInfo customer,
        List<Line> orderItems,
        List<PaymentLine> payments) {

    public record CustomerInfo(String firstName, String lastName, String phone, String location) {}

    public record Line(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subTotal) {}

    public record PaymentLine(Long id, String method, BigDecimal amount, String status, String verificationStatus,
            String reference, LocalDateTime paidAt, String recordedBy, String note) {}
}
