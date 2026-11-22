package com.smart.faidahalisi.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Rekodi ya malipo. Oda moja inaweza kuwa na malipo MENGI (malipo ya sehemu).
 *
 * Hali mbili tofauti (usizichanganye):
 *  - stutus (jina la zamani, tahajia ya zamani imebaki ili database isibadilike):
 *      PENDING  = mteja ameonyesha njia ya malipo, pesa bado HAIJARekodiwa
 *      RECEIVED = mmiliki ameandika kuwa pesa imepokelewa
 *      FAILED   = malipo yameshindikana / yamekataliwa
 *  - verificationStatus:
 *      UNVERIFIED = bado haijahakikiwa; HAIHESABIWI kwenye salio
 *      VERIFIED   = imehakikiwa; ndiyo pekee inayopunguza deni la oda
 *      REJECTED   = imekataliwa baada ya ukaguzi
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "method", nullable = false)
    private String method;
    private String stutus;
    private String currency;
    private LocalDateTime paidAt;
    private String transactionalRefference;

    // Oda ambayo malipo haya ni yake (null kwa rekodi za zamani sana kabla ya mabadiliko haya).
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private Orders order;

    @Column(name = "verification_status", length = 20)
    private String verificationStatus;
    @Column(name = "recorded_by", length = 180)
    private String recordedBy;
    @Column(name = "verified_by", length = 180)
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    @Column(length = 255)
    private String note;

    // Uhusiano wa zamani (Orders.payment). Haitumiki kwa malipo mapya.
    @JsonIgnore
    @OneToOne(mappedBy = "payment")
    private Orders orders;

    public Payment() {
        // JPA inahitaji constructor tupu.
    }
}
