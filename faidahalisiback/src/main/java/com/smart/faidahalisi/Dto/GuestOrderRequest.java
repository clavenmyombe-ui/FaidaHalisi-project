package com.smart.faidahalisi.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Oda ya mteja asiye na akaunti: taarifa zake zinahifadhiwa kwenye Customer. */
public record GuestOrderRequest(
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotBlank @Pattern(regexp = "^[+0-9 ()-]{9,20}$", message = "Namba ya simu si sahihi.") String phone,
        @Size(max = 120) String location,
        @NotBlank @Size(max = 255) String deliveryAddress,
        @NotEmpty @Size(max = 50) List<@Valid Item> items) {

    public record Item(@NotNull Long productId, @Min(1) @Max(1000) int quantity) {
    }
}
