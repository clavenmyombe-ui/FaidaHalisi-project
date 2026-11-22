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

/** Bidhaa iliyorudishwa na mteja baada ya oda kufikishwa. Inapunguza mapato, si kufuta oda. */
@Entity
@Table(name = "order_returns")
@Getter
@Setter
public class OrderReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(nullable = false)
    private int quantity;
    // Kiasi cha pesa kinachorudishwa/kupunguzwa kwa mteja (unitPrice * quantity).
    @Column(nullable = false)
    private BigDecimal creditAmount;
    // Gharama ya bidhaa hizo wakati wa mauzo (kwa kukokotoa faida/hasara).
    private BigDecimal unitCost;
    // true = bidhaa ni nzima na imerudishwa kwenye stock; false = imeharibika (hasara).
    @Column(nullable = false)
    private boolean restocked;
    @Column(nullable = false, length = 255)
    private String reason;
    @Column(nullable = false, length = 180)
    private String recordedBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
