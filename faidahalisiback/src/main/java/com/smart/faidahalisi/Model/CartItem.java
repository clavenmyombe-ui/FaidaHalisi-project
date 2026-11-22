package com.smart.faidahalisi.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table (name = "Cart_Item")
@Getter 
@Setter 
@AllArgsConstructor 
public class CartItem {
@GeneratedValue (strategy = GenerationType.IDENTITY)
@Id 
private Long id;

private int quantity;
@ManyToOne 
@JoinColumn (name = "cart_id", nullable = false)
private Cart cart;

@ManyToOne 
@JoinColumn (name = "product_id", nullable = false)
private Products products;

public CartItem(){}
}
