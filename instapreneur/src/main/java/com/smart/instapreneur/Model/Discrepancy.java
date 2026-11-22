package com.smart.instapreneur.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Tofauti inayohitaji UCHUNGUZI (stock au malipo). Si ushahidi wa wizi:
 * inaweza kuwa kosa la kuhesabu, kuandika, au kupokea. Mmiliki anaifunga kwa maelezo.
 */
@Entity
@Table(name = "discrepancies")
@Getter
@Setter
public class Discrepancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long storeId;
    // STOCK au PAYMENT
    @Column(nullable = false, length = 20)
    private String kind;
    @Column(length = 60)
    private String reference;
    @Column(nullable = false, length = 500)
    private String description;
    // OPEN au RESOLVED
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, length = 180)
    private String createdBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(length = 180)
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    @Column(length = 255)
    private String resolutionNote;
}
