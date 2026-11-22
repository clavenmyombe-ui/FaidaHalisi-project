package com.smart.instapreneur.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import com.fasterxml.jackson.annotation.JsonProperty;
@Entity 
@Getter 
@Setter 
@AllArgsConstructor 

public class User {
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Id 
private Long id;
@Column(name = "first_name", nullable = false)
private String firstName;
@Column(name = "last_name", nullable = false)
private String lastName;
@Column(unique = true, nullable = false)
private String email;
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
private String role;
public User() {
    // Default constructor  
}
}
