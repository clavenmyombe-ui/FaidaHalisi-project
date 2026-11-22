package com.smart.instapreneur.Dto;

import java.math.BigDecimal;

/** Bidhaa za umma hazijumuishi stock halisi ya kampuni. */
public record PublicProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        String status,
        String category,
        String description,
        PublicStore store) {
    public record PublicStore(Long id, String storeName, String slug, String description, String logoUrl) {}
}
