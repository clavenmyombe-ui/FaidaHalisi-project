package com.smart.instapreneur.Dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * "Nimepokea pesa": muuzaji anaandika namba ya muamala (kutoka ujumbe wa M-Pesa / Mixx / Airtel / HaloPesa)
 * na, kwa malipo ya sehemu, kiasi kilichopokelewa. Namba ya muamala haiwezi kutumika mara mbili.
 * Kitendo hiki kinarekodi tu; uthibitisho ni hatua tofauti (POST /api/payments/{id}/verify).
 */
public record MarkPaidRequest(
        @NotBlank(message = "Weka namba ya muamala.") @Size(max = 100) String transactionReference,
        BigDecimal amount) {
}
