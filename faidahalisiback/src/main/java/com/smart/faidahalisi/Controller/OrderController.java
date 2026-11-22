package com.smart.faidahalisi.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.smart.faidahalisi.Dto.OrderResponse;
import com.smart.faidahalisi.Model.Customer;
import com.smart.faidahalisi.Model.Orders;
import com.smart.faidahalisi.Security.OwnershipService;
import com.smart.faidahalisi.Service.CustomerService;
import com.smart.faidahalisi.Service.OrderService;
import com.smart.faidahalisi.Service.OrderViewService;
import com.smart.faidahalisi.Service.OrderWorkflowService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Oda kwa muuzaji na mteja mwenye akaunti. Majibu yote ni OrderResponse (data salama), si entity.
 * Kubadilisha hali ya oda kunapita OrderWorkflowService tu; hapa tunakagua ruhusa (@PreAuthorize)
 * kisha tunaita huduma.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final CustomerService customerService;
    private final OrderWorkflowService workflow;
    private final OrderViewService view;
    private final OwnershipService ownership;

    public OrderController(OrderService orderService, CustomerService customerService,
            OrderWorkflowService workflow, OrderViewService view, OwnershipService ownership) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.workflow = workflow;
        this.view = view;
        this.ownership = ownership;
    }

    // POST /api/orders/cart/{cartId}: oda kutoka cart ya akaunti ya JWT (umiliki unakaguliwa ndani ya huduma).
    @PostMapping("/cart/{cartId}")
    public ResponseEntity<OrderResponse> createOrder(@PathVariable Long cartId,
            @RequestBody DeliveryAddressRequest request, Authentication authentication) {
        Orders order = orderService.createOrder(cartId, request.deliveryAddress(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(view.toResponse(order));
    }

    // Oda inaweza kuonwa na mteja wake, mmiliki wa duka, au admin.
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderOwner(#p0, authentication) or @customerAuthorization.isOrderOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(view.toResponse(orderService.getOrderById(orderId)));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderNumberOwner(#p0, authentication) or @customerAuthorization.isOrderNumberOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(view.toResponse(orderService.getOrderByOrderNumber(orderNumber)));
    }

    // Muuzaji anaona oda za duka lake tu.
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isStoreOwner(#p0, authentication)")
    public ResponseEntity<List<OrderResponse>> getOrdersByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(view.toResponses(orderService.getOrdersByStore(storeId)));
    }

    // customerId haichukuliwi kwenye URL; inatoka kwenye akaunti ya JWT.
    @GetMapping("/mine")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        Customer customer = customerService.getMyCustomer(authentication.getName());
        return ResponseEntity.ok(view.toResponses(orderService.getOrdersByCustomer(customer.getId())));
    }

    // Mmiliki anakagua upatikanaji na kuidhinisha: stock inashikiliwa hapa (si wakati wa kuweka oda).
    @PostMapping({"/{orderId}/approve", "/{orderId}/approve-stock"})
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> approve(@PathVariable Long orderId, Authentication authentication) {
        return ResponseEntity.ok(view.toResponse(workflow.approve(orderId, authentication.getName())));
    }

    // Kukataa oda ambayo bado haijaidhinishwa (mfano stock haitoshi), kwa sababu iliyoandikwa.
    @PostMapping("/{orderId}/reject")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> reject(@PathVariable Long orderId,
            @RequestBody ReasonRequest request, Authentication authentication) {
        return ResponseEntity.ok(view.toResponse(workflow.reject(orderId, request.reason(), authentication.getName())));
    }

    // Hatua za mbele (PROCESSING, READY_FOR_DELIVERY, DELIVERED, COMPLETED). Jedwali la mabadiliko liko kwenye huduma.
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long orderId,
            @RequestBody StatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(view.toResponse(workflow.advance(orderId, request.status(), authentication.getName())));
    }

    // Muuzaji anaghairi oda (kabla ya kufikishwa) kwa sababu iliyoandikwa; stock iliyoshikiliwa inarudi mara moja.
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> cancelBySeller(@PathVariable Long orderId,
            @RequestBody ReasonRequest request, Authentication authentication) {
        return ResponseEntity.ok(view.toResponse(workflow.cancel(orderId, request.reason(), authentication.getName(), false)));
    }

    // Mteja mwenye akaunti anaweza kughairi oda yake kabla haijaidhinishwa; muuzaji/admin pia kupitia njia hii ya zamani.
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderOwner(#p0, authentication) or @customerAuthorization.isOrderOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId,
            @RequestParam(required = false) String reason, Authentication authentication) {
        boolean seller = ownership.isOrderOwner(orderId, authentication)
                || authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(view.toResponse(workflow.cancel(orderId, reason, authentication.getName(), !seller)));
    }

    // Mteja amerudisha bidhaa baada ya kufikishwa (inapunguza mapato; bidhaa nzima inarudi kwenye stock).
    @PostMapping("/{orderId}/returns")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isOrderOwner(#p0, authentication)")
    public ResponseEntity<OrderResponse> recordReturn(@PathVariable Long orderId,
            @Valid @RequestBody ReturnRequest request, Authentication authentication) {
        workflow.recordReturn(orderId, request.productId(), request.quantity(), request.reason(),
                request.restock(), authentication.getName());
        return ResponseEntity.ok(view.toResponse(orderService.getOrderById(orderId)));
    }

    public record DeliveryAddressRequest(String deliveryAddress) {}
    public record StatusRequest(String status) {}
    public record ReasonRequest(@Size(max = 255) String reason) {}
    public record ReturnRequest(@NotNull Long productId, @Min(1) int quantity,
            @Size(max = 255) String reason, boolean restock) {}
}
