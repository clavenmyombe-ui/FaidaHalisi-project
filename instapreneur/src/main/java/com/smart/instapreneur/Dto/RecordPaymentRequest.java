package com.smart.instapreneur.Dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Muuzaji anarekodi malipo (kamili au ya sehemu) yaliyopokelewa kwa oda. */
public record RecordPaymentRequest(
        @NotNull(message = "Weka kiasi.") BigDecimal amount,
        @NotBlank(message = "Weka njia ya malipo.") @Size(max = 20) String method,
        @Size(max = 100) String transactionReference,
        @Size(max = 255) String note) {
}
