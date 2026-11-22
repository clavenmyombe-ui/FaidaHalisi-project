package com.smart.instapreneur.Model;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table (name = "Order_items")
@Getter 
@Setter 
@AllArgsConstructor 
public class OrderItems {
@GeneratedValue (strategy = GenerationType.IDENTITY)
@Id 
private Long id;
private int quantity;
private BigDecimal unitPrice;
private BigDecimal subTotal;
// Gharama ya kipande WAKATI WA mauzo (inachukuliwa stock inaposhikiliwa). Inahifadhiwa ili
// mabadiliko ya baadaye ya gharama yasibadilishe faida ya kihistoria. null = gharama haikujulikana.
@Column(name = "unit_cost")
private BigDecimal unitCost;
@JsonIgnore // The parent order already contains its items; do not serialize back to it.
@ManyToOne
@JoinColumn (name = "order_id", nullable = false)
private Orders order;
@ManyToOne
@JoinColumn (name = "product_id", nullable = false)
private Products product;
public OrderItems() {
    // Default constructor
}
}
