package com.smart.faidahalisi.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GuestOrderResponse(
        Long id,
        String orderNumber,
        String trackingToken,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        boolean stockNeedsReview,
        String message) {
}
