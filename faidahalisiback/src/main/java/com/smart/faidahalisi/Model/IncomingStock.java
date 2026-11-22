package com.smart.faidahalisi.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Mzigo UNAOTARAJIWA kufika. Haubadilishi stock mpaka mmiliki authibitishe umepokelewa
 * (receive). Hii inatenganisha "tumeagiza" na "tumepokea".
 * Hali: EXPECTED -> RECEIVED au CANCELLED.
 */
@Entity
@Table(name = "incoming_stock")
@Getter
@Setter
public class IncomingStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(nullable = false)
    private int expectedQuantity;
    private int receivedQuantity;
    // Gharama ya kipande kilichokubaliwa na msambazaji (inaweza kurekebishwa wakati wa kupokea).
    private BigDecimal unitCost;
    @Column(length = 120)
    private String supplier;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(length = 255)
    private String note;
    @Column(nullable = false, length = 180)
    private String createdBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(length = 180)
    private String receivedBy;
    private LocalDateTime receivedAt;
}
