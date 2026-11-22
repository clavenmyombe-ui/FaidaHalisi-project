package com.smart.faidahalisi.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Entity 
@Getter 
@Setter 
@AllArgsConstructor 
public class Store {
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Id 
private Long id;
@Column (name = "store_name", nullable = false)
private String storeName;
// Usalama: JSON ya umma ya duka isionyeshe email/jina/role ya muuzaji; kitambulisho (id) tu.
@JsonIgnoreProperties({"email", "firstName", "lastName", "role", "password"})
@OneToOne 
@JoinColumn (name = "seller_id", nullable = false)
private User seller;
// Namba ya WhatsApp ya biashara (mfano 255712345678) kwa msaada wa wateja.
@Column(name = "whatsapp_number", length = 20)
private String whatsappNumber;
@Column (name = "slug", nullable = false, unique = true)
private String slug;
// Picha inatumwa kama data URL (ndefu): MEDIUMTEXT, si VARCHAR(255).
@Column(name = "logo_url", columnDefinition = "MEDIUMTEXT")
private String logoUrl;
private String description;

public Store() {
    // Default constructor  
}


}