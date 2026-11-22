package com.smart.instapreneur.Model;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table (name = "cart")
@Getter 
@Setter 
@AllArgsConstructor 
public class Cart {
@GeneratedValue (strategy = GenerationType.IDENTITY)
@Id 
private Long id;

@OneToOne
@JoinColumn (name = "customer_id", nullable = true)
private Customer customer;

@Column(name = "guest_key", unique = true, length = 36)
private String guestKey;


public Cart(){}
}
