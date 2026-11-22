package com.smart.instapreneur.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/** Namba ya oda (inaonekana kwenye risiti) na token ya siri ya tracking (haitabiriki). */
public final class OrderIdentifiers {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OrderIdentifiers() {}

    public static String newOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Bytes 24 za SecureRandom = herufi 32; haiwezi kubashiriwa kama ID ya 1, 2, 3. */
    public static String newTrackingToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
