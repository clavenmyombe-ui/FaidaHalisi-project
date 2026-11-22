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
 * Historia ya vitendo muhimu: nani, nini, kabla, baada, lini, kwa nini.
 * Rekodi hizi haziandikwi upya wala kufutwa na code ya app (AuditService inaongeza tu).
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long storeId;
    @Column(nullable = false, length = 180)
    private String actorEmail;
    @Column(nullable = false, length = 40)
    private String action;
    @Column(nullable = false, length = 30)
    private String entityType;
    @Column(length = 60)
    private String entityId;
    @Column(length = 500)
    private String oldValue;
    @Column(length = 500)
    private String newValue;
    @Column(length = 255)
    private String reason;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
