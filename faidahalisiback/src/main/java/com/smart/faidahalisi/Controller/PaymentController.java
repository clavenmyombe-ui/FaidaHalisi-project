package com.smart.faidahalisi.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.faidahalisi.Dto.MarkPaidRequest;
import com.smart.faidahalisi.Dto.OrderResponse;
import com.smart.faidahalisi.Dto.PaymentRequestDto;
import com.smart.faidahalisi.Dto.RecordPaymentRequest;
import com.smart.faidahalisi.Model.Orders;
import com.smart.faidahalisi.Model.Payment;
import com.smart.faidahalisi.Service.OrderService;
import com.smart.faidahalisi.Service.OrderViewService;
import com.smart.faidahalisi.Service.PaymentService;

import jakarta.validation.Valid;

/**
 * Malipo ya oda (mobile money ya mkono, bila gateway).
 *
 *   1. Mteja anachagua njia (hiari): POST /api/payments/order-number/{orderNumber}  -> PENDING, hakuna pesa iliyohesabiwa.
 *   2. Muuzaji anarekodi pesa aliyopokea (kamili au sehemu):
 *        PUT  /api/payments/{id}/paid              (kwa chaguo la mteja lililopo)
 *        POST /api/payments/orders/{orderId}/record (malipo mapya moja kwa moja)
 *   3. Muuzaji anahakiki: POST /api/payments/{id}/verify  -> ndipo salio la oda linapungua.
 *   4. Au anakataa: POST /api/payments/{id}/reject -> kesi ya uchunguzi inafunguliwa.
 *
 * Gateway halisi ikiongezwa baadaye, hatua ya 3 itafanywa na uthibitisho wa gateway (webhook), si mtu.
 * Hadi hapo, "VERIFIED" inamaanisha mmiliki amehakiki mwenyewe; app haidai kuwa benki/mtandao umethibitisha.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final String OWNER_OF_PAYMENT = "hasRole('ADMIN') or (hasRole('SELLER') and @paymentAuthorization.isPaymentStoreOwner(#p0, authentication))";

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final OrderViewService view;

    public PaymentController(PaymentService paymentService, OrderService orderService, OrderViewService view) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.view = view;
    }

    // Wazi kwa kila mtu: mteja asiye na akaunti anachagua njia ya malipo. Kiasi kinahesabiwa na backend.
    @PostMapping("/order-number/{orderNumber}")
    public ResponseEntity<Payment> createPaymentForOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody PaymentRequestDto request) {
        Orders order = orderService.getOrderByOrderNumber(orderNumber);
        Payment payment = paymentService.createPayment(order.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    // Muuzaji anarekodi malipo mapya (kamili au ya sehemu) kwa oda ya duka lake.
    @PostMapping("/orders/{orderId}/record")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and @ownership.isOrderOwner(#p0, authentication))")
    public ResponseEntity<OrderResponse> recordPayment(@PathVariable Long orderId,
            @Valid @RequestBody RecordPaymentRequest request, Authentication authentication) {
        paymentService.recordPayment(orderId, request.amount(), request.method(), request.transactionReference(),
                request.note(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(view.toResponse(orderService.getOrderById(orderId)));
    }

    // "Nimepokea pesa" kwa chaguo la malipo la mteja (bado haijahakikiwa).
    @PutMapping("/{paymentId}/paid")
    @PreAuthorize(OWNER_OF_PAYMENT)
    public ResponseEntity<Payment> markAsPaid(@PathVariable Long paymentId,
            @Valid @RequestBody MarkPaidRequest request, Authentication authentication) {
        return ResponseEntity.ok(paymentService.markPaymentAsPaid(paymentId, request.transactionReference(),
                request.amount(), authentication.getName()));
    }

    @PostMapping("/{paymentId}/verify")
    @PreAuthorize(OWNER_OF_PAYMENT)
    public ResponseEntity<Payment> verify(@PathVariable Long paymentId, Authentication authentication) {
        return ResponseEntity.ok(paymentService.verifyPayment(paymentId, authentication.getName()));
    }

    @PostMapping("/{paymentId}/reject")
    @PreAuthorize(OWNER_OF_PAYMENT)
    public ResponseEntity<Payment> reject(@PathVariable Long paymentId, @RequestBody ReasonRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.rejectPayment(paymentId, request.reason(), authentication.getName()));
    }

    // Malipo yameshindikana (haifuti historia).
    @PutMapping("/{paymentId}/failed")
    @PreAuthorize(OWNER_OF_PAYMENT)
    public ResponseEntity<Payment> markAsFailed(@PathVariable Long paymentId, Authentication authentication) {
        return ResponseEntity.ok(paymentService.markPaymentAsFailed(paymentId, authentication.getName()));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize(OWNER_OF_PAYMENT)
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    // Malipo yote kwenye mfumo: ADMIN pekee (yana taarifa za maduka yote).
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/transaction/{reference}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Payment> getByTransaction(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getPaymentByTransaction(reference));
    }

    public record ReasonRequest(String reason) {}
}
