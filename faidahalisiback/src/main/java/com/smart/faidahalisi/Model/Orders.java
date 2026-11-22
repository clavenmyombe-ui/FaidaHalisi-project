package com.smart.faidahalisi.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table (name = "orders")
@Getter 
@Setter 
@AllArgsConstructor
public class Orders {
@GeneratedValue (strategy = GenerationType.IDENTITY)
@Id
private Long id;

private String orderNumber;

//Hii ni token ya siri ya ufuatiliaji; 
@Column(name = "tracking_token", unique = true, length = 64)
private String trackingToken;

// ikiwa true = stock ya oda hii imeshikiliwa (reserved) na bado haijatoka ghalani.
@Column(name = "stock_reserved", nullable = false)
private boolean stockReserved;
//ikiwa true = bidhaa zimetolewa ghalani (oda imefikishwa). Inazuia kupunguza stock mara mbili.
@Column(name = "stock_issued", nullable = false)
private boolean stockIssued;
private LocalDateTime deliveredAt;
private LocalDateTime cancelledAt;
@Column(length = 255)
private String cancelReason;
// Imeghairiwa baada ya stock kushikiliwa: inaonyeshwa kwa mmiliki kama jambo la kuchunguza.
@Column(name = "cancelled_after_reservation", nullable = false)
private boolean cancelledAfterReservation;
private String status;
private BigDecimal totalAmount;
private String deliveryAddress;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;

@ManyToOne 
@JoinColumn(name="store_id", nullable = false)
private Store store;

@ManyToOne
@JoinColumn (name = "customer_id", nullable = false)
private Customer customer;

// KUMBUKA: field hii ni ya zamani (malipo moja kwa oda). Malipo mapya yanatumia Payment.order
// (malipo mengi kwa oda moja, kwa ajili ya malipo ya sehemu). Usiitumie kwa code mpya.
@JsonIgnore
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn (name = "payment_id", nullable = true)
private Payment payment;

@OneToMany (mappedBy = "order", cascade = CascadeType.ALL)
private List<OrderItems> orderItems;

public Orders() {
    // Default constructor
}
}
