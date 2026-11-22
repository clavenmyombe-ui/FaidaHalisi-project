package com.smart.faidahalisi.Service;

import java.util.List;

import com.smart.faidahalisi.Model.Orders;

/**
 * Hali za oda na jedwali la mabadiliko. Hali zote ziko mahali pamoja ili kila sehemu ya
 * app itumie maana ile ile.
 *
 * Njia ya kawaida:
 *   NEW -> (STOCK_REVIEW) -> APPROVED -> PROCESSING -> READY_FOR_DELIVERY -> DELIVERED -> COMPLETED
 * Kwa wakati wowote kabla ya DELIVERED: -> CANCELLED. Baada ya DELIVERED: hakuna kughairi
 * (tumia kurudisha bidhaa / returns).
 */
public final class OrderStatus {
    private OrderStatus() {}

    public static final String NEW = "NEW";
    public static final String STOCK_REVIEW = "STOCK_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String PROCESSING = "PROCESSING";
    public static final String READY_FOR_DELIVERY = "READY_FOR_DELIVERY";
    public static final String DELIVERED = "DELIVERED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    public static final List<String> AWAITING_REVIEW = List.of(NEW, STOCK_REVIEW);
    public static final List<String> AWAITING_DELIVERY = List.of(APPROVED, PROCESSING, READY_FOR_DELIVERY);
    public static final List<String> SOLD = List.of(DELIVERED, COMPLETED);

    /**
     * Hali halisi ya oda. Oda za zamani zilitumia majina mengine (PENDING, CONFIRMED, SHIPPED,
     * PAYMENT_PENDING, PAID...). Tunazibadilisha kuwa hali mpya ili jedwali la mabadiliko lifanye
     * kazi bila kuhitaji kubadilisha database yote.
     */
    public static String effective(Orders order) {
        String raw = order.getStatus() == null ? NEW : order.getStatus().trim().toUpperCase();
        switch (raw) {
            case "PENDING":
            case "PAYMENT_PENDING":
            case "PAYMENT_FAILED":
            case "PAID":
                return order.isStockReserved() ? APPROVED : NEW;
            case "CONFIRMED":
                return APPROVED;
            case "SHIPPED":
                return READY_FOR_DELIVERY;
            default:
                return raw;
        }
    }

    /** Kiungo cha tracking cha mteja kinafungwa oda ikifika hali hizi. Rekodi za kampuni zinabaki. */
    public static boolean trackingClosed(String status) {
        return DELIVERED.equals(status) || COMPLETED.equals(status) || CANCELLED.equals(status);
    }

    /** Maelezo mafupi ya Kiswahili kwa mteja. */
    public static String labelSw(String status) {
        switch (status) {
            case NEW: return "Oda imepokelewa, inasubiri kukaguliwa";
            case STOCK_REVIEW: return "Inasubiri kuthibitisha upatikanaji wa bidhaa";
            case APPROVED: return "Oda imekubaliwa";
            case PROCESSING: return "Oda inaandaliwa";
            case READY_FOR_DELIVERY: return "Oda iko tayari kupelekwa";
            case DELIVERED: return "Oda imefikishwa";
            case COMPLETED: return "Oda imekamilika";
            case CANCELLED: return "Oda imeghairiwa";
            default: return status;
        }
    }
}
