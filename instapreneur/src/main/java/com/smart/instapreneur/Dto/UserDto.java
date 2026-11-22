package com.smart.instapreneur.Dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class UserDto {
@NotBlank(message = "Andika jina la kwanza")
private String firstName;
@NotBlank(message = "Andika jina la mwisho")
private String lastName;
@NotBlank(message = "Andika email")
@Email(message = "Andika email sahihi")
private String email;    
}
