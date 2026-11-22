package com.smart.instapreneur.Model;

/**
 * Majina ya aina za harakati za stock. Majina ya zamani (STOCK_RECEIVED, ORDER_RESERVED,
 * ORDER_CANCELLED_RETURN) yamebaki ili historia ya zamani iendelee kueleweka.
 */
public final class MovementType {
    private MovementType() {}

    public static final String STOCK_RECEIVED = "STOCK_RECEIVED";          // mzigo umepokelewa
    public static final String ORDER_RESERVED = "ORDER_RESERVED";          // stock imeshikiliwa kwa oda
    public static final String ORDER_RELEASED = "ORDER_CANCELLED_RETURN";  // oda imeghairiwa, stock imeachiliwa
    public static final String ORDER_ISSUED = "ORDER_ISSUED";              // bidhaa zimetoka ghalani (delivery)
    public static final String CUSTOMER_RETURN = "CUSTOMER_RETURN";        // mteja amerudisha bidhaa nzima
    public static final String RETURN_DAMAGED = "RETURN_DAMAGED";          // mteja amerudisha bidhaa iliyoharibika
    public static final String STOCK_DAMAGED = "STOCK_DAMAGED";            // bidhaa imeharibika ghalani
    public static final String STOCK_LOST = "STOCK_LOST";                  // bidhaa imepotea
    public static final String STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT";      // marekebisho ya mmiliki (yenye sababu)
    public static final String COUNT_DIFFERENCE = "COUNT_DIFFERENCE";      // tofauti kwenye hesabu ya ghalani
}
