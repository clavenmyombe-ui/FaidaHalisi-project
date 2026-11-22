package com.smart.faidahalisi.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.faidahalisi.Dto.GuestOrderRequest;
import com.smart.faidahalisi.Dto.GuestOrderResponse;
import com.smart.faidahalisi.Dto.GuestOrderTrackingResponse;
import com.smart.faidahalisi.Service.GuestOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class GuestOrderController {

    private final GuestOrderService guestOrderService;

    public GuestOrderController(GuestOrderService guestOrderService) {
        this.guestOrderService = guestOrderService;
    }

    // Wazi kwa kila mtu (angalia SecurityConfig): mteja haitaji akaunti.
    // POST /api/orders/guest huweka oda bila login; server huhesabu bei kutoka bidhaa za database.
    @PostMapping("/guest")
    public ResponseEntity<GuestOrderResponse> placeGuestOrder(@Valid @RequestBody GuestOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestOrderService.placeOrder(request));
    }

    // Token ni siri isiyotabirika na inaruhusu kuona oda moja tu. Hufungwa oda ikikamilika.
    @GetMapping("/track/{token}")
    public ResponseEntity<GuestOrderTrackingResponse> trackGuestOrder(@PathVariable String token) {
        return ResponseEntity.ok(guestOrderService.trackOrder(token));
    }
}
