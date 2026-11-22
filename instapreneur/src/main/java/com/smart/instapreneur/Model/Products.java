package com.smart.instapreneur.Model;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name = "products") 
@Getter 
@Setter 
@AllArgsConstructor 
public class Products {
@GeneratedValue (strategy = GenerationType.IDENTITY)
@Id 
private Long id;
@Column(name = "product_name", nullable = false)
private String name;
private BigDecimal price;

private int stockQuantity;

@Column(name = "cost_price")
private BigDecimal costPrice;

@Column(name = "image_url", columnDefinition = "MEDIUMTEXT")
private String imageUrl;
private String status;
private String category;
@ManyToOne 
@JoinColumn (name = "store_id", nullable = false)
private Store store;
@JsonIgnore // this is to prevents a product response from looping through its past orders.
@OneToMany (mappedBy = "product")
private List<OrderItems> orderItems;
@Column (name = "product_description", nullable = true)
private String description;

public Products() {
    // Default constructor  
}
}
