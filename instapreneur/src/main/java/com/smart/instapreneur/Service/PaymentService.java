package com.smart.instapreneur.Service;

import java.math.BigDecimal;
import java.util.List;

import com.smart.instapreneur.Dto.PaymentRequestDto;
import com.smart.instapreneur.Model.Payment;

public interface PaymentService {

    // Returns all payments in the database.
    List<Payment> getAllPayments();

    // Finds one payment by id.
    Payment getPaymentById(Long paymentId);

    // Finds one payment using the transaction reference from the provider or mock provider.
    Payment getPaymentByTransaction(String transactionalRefference);

    // Mteja anaonyesha njia ya malipo (PENDING, hakuna pesa iliyorekodiwa bado).
    Payment createPayment(Long orderId, PaymentRequestDto paymentRequestDto);

    // Muuzaji anaandika kuwa amepokea pesa kwa malipo yaliyokuwa PENDING (bado haijahakikiwa).
    Payment markPaymentAsPaid(Long paymentId, String transactionalRefference, BigDecimal amount, String actor);

    // Marks a payment as failed without deleting its history.
    Payment markPaymentAsFailed(Long paymentId, String actor);

    // Muuzaji anarekodi malipo mapya (kamili au ya sehemu) moja kwa moja kwa oda.
    Payment recordPayment(Long orderId, BigDecimal amount, String method, String reference, String note, String actor);

    // Uthibitisho: malipo yanaanza kuhesabiwa kwenye salio la oda.
    Payment verifyPayment(Long paymentId, String actor);

    // Kukataa malipo baada ya ukaguzi (yanafunguliwa kama jambo la kuchunguza).
    Payment rejectPayment(Long paymentId, String reason, String actor);
}
