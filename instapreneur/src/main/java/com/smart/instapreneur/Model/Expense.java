package com.smart.instapreneur.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
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

/** Gharama za uendeshaji (usafiri, kodi, mshahara...). Hazijumuishi gharama ya kununua bidhaa. */
@Entity
@Table(name = "expenses")
@Getter
@Setter
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(length = 255)
    private String description;
    @Column(nullable = false)
    private LocalDate expenseDate;
    @Column(nullable = false, length = 180)
    private String recordedBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
