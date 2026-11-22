package com.smart.faidahalisi.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Historia isiyofutwa ya mabadiliko ya stock; husaidia kufuatilia tofauti za hesabu. */
@Entity
@Table(name = "inventory_movements")
@Getter @Setter
public class InventoryMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    // Aina: angalia MovementType (STOCK_RECEIVED, ORDER_RESERVED, ORDER_RELEASED, ORDER_ISSUED, ...)
    @Column(nullable = false, length = 30)
    private String movementType;
    @Column(nullable = false)
    private int quantityChange; // chanya kwa stock inayoingia; hasi kwa upungufu uliorekodiwa
    @Column(nullable = false)
    private int quantityBefore;
    @Column(nullable = false)
    private int quantityAfter;
    // Idadi ya vipande vilivyohusika (daima chanya). quantityChange ni badiliko la stock INAYOPATIKANA
    // (kwa mfano ORDER_ISSUED haibadilishi stock inayopatikana lakini vipande vimetoka ghalani).
    @Column(nullable = false)
    private int quantity;
    // Gharama ya kipande wakati wa harakati (kwa kukokotoa hasara ya bidhaa zilizoharibika/kupotea).
    @Column(name = "unit_cost")
    private java.math.BigDecimal unitCost;
    // Kumbukumbu inayohusiana: namba ya oda, namba ya mapokezi, n.k.
    @Column(length = 60)
    private String reference;
    @Column(nullable = false, length = 255)
    private String reason;
    @Column(nullable = false, length = 180)
    private String actorEmail;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
