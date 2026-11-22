BACKEND (Spring Boot) - ongeza / badilisha hivi
(package ni com.smart.faidahalisi, folda ni src/main/java/com/smart/faidahalisi/...)

FAILI MPYA (6) - nakili kwenye folda zake:
  Dto/GuestOrderRequest.java        -> Dto/
  Dto/GuestOrderResponse.java       -> Dto/
  Dto/MarkPaidRequest.java          -> Dto/
  Service/GuestOrderService.java    -> Service/      (oda ya mgeni; imerekebishwa ORD- na exception yako)
  Controller/GuestOrderController.java -> Controller/
  Controller/PaymentController.java -> Controller/   (MALIPO, ina maelezo ya Kiswahili)
  Security/PaymentAuthorization.java -> Security/    (inakagua muuzaji wa duka)

MABADILIKO MADOGO KwENYE FAILI ZAKO:
1) CustomerRepository.java - ongeza (na import java.util.Optional):
      Optional<Customer> findByPhone(String phone);

2) SecurityConfig.java - ongeza mistari hii miwili chini ya
   .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
      .requestMatchers(HttpMethod.POST, "/api/orders/guest").permitAll()
      .requestMatchers(HttpMethod.POST, "/api/payments/order-number/*").permitAll()

3) PaymentRequestDto.java - ongeza @NoArgsConstructor (juu ya @AllArgsConstructor) na import lombok.NoArgsConstructor.
   Sababu: Jackson inahitaji constructor tupu kusoma JSON ya mwili wa ombi.

Kisha run backend upya.
