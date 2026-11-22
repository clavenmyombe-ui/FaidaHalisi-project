package com.smart.faidahalisi.Dto;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
public class PaymentRequestDto {
private BigDecimal amount;
private String currency;
private String method;
}
