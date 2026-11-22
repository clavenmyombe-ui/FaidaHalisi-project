package com.smart.faidahalisi.Controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smart.faidahalisi.Dto.InventoryDtos;
import com.smart.faidahalisi.Dto.InventoryMovementResponse;
import com.smart.faidahalisi.Model.InventoryMovement;
import com.smart.faidahalisi.Repository.InventoryMovementRepository;
import com.smart.faidahalisi.Repository.ProductRepository;
import com.smart.faidahalisi.Service.InventoryService;

/**
 * Stock kwa mmiliki: mapokezi, mzigo unaotarajiwa, marekebisho, hasara, hesabu ya ghalani na historia.
 * Mabadiliko yote yanapita InventoryService (lock + sheria + historia + audit).
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private static final String OWNER_OF_PRODUCT = "hasRole('ADMIN') or (hasRole('SELLER') and @ownership.isProductOwner(#p0, authentication))";
    private static final String OWNER_OF_STORE = "hasRole('ADMIN') or (hasRole('SELLER') and @ownership.isStoreOwner(#p0, authentication))";
    private static final String OWNER_OF_INCOMING = "hasRole('ADMIN') or (hasRole('SELLER') and @ownership.isIncomingOwner(#p0, authentication))";

    private final InventoryService inventory;
    private final ProductRepository products;
    private final InventoryMovementRepository movements;

    public InventoryController(InventoryService inventory, ProductRepository products, InventoryMovementRepository movements) {
        this.inventory = inventory;
        this.products = products;
        this.movements = movements;
    }

    // ---- Muhtasari wa duka ----

    /** Kwa kila bidhaa: onHand, available, reserved, incoming, gharama na thamani. */
    @GetMapping("/store/{storeId}/overview")
    @PreAuthorize(OWNER_OF_STORE)
    public List<InventoryDtos.StockRow> overview(@PathVariable Long storeId) {
        return inventory.overview(storeId);
    }

    @GetMapping("/store/{storeId}/movements")
    @PreAuthorize(OWNER_OF_STORE)
    public List<InventoryMovementResponse> storeMovements(@PathVariable Long storeId) {
        return movements.findTop200ByProductStoreIdOrderByCreatedAtDesc(storeId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/store/{storeId}/incoming")
    @PreAuthorize(OWNER_OF_STORE)
    public List<InventoryDtos.IncomingResponse> incoming(@PathVariable Long storeId) {
        return inventory.incomingForStore(storeId);
    }

    // ---- Bidhaa moja ----

    /** Kupokea stock mara moja (mzigo umefika). Gharama ya kipande ni hiari lakini inahitajika kwa faida sahihi. */
    @PostMapping("/products/{productId}/receive")
    @PreAuthorize(OWNER_OF_PRODUCT)
    public InventoryMovementResponse receiveStock(@PathVariable Long productId, @RequestBody ReceiveRequest request, Authentication auth) {
        return toResponse(inventory.receive(productId, request.quantity(), request.unitCost(), request.reason(), auth.getName(), null));
    }

    @GetMapping("/products/{productId}/history")
    @PreAuthorize(OWNER_OF_PRODUCT)
    public List<InventoryMovementResponse> history(@PathVariable Long productId) {
        if (!products.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bidhaa haijapatikana.");
        }
        return movements.findByProductIdOrderByCreatedAtDesc(productId).stream().map(this::toResponse).toList();
    }

    @PostMapping("/products/{productId}/adjust")
    @PreAuthorize(OWNER_OF_PRODUCT)
    public InventoryMovementResponse adjust(@PathVariable Long productId, @RequestBody AdjustRequest request, Authentication auth) {
        return toResponse(inventory.adjust(productId, request.delta(), request.reason(), auth.getName()));
    }

    @PostMapping("/products/{productId}/write-off")
    @PreAuthorize(OWNER_OF_PRODUCT)
    public InventoryMovementResponse writeOff(@PathVariable Long productId, @RequestBody WriteOffRequest request, Authentication auth) {
        return toResponse(inventory.writeOff(productId, request.type(), request.quantity(), request.reason(), auth.getName()));
    }

    @PostMapping("/products/{productId}/count")
    @PreAuthorize(OWNER_OF_PRODUCT)
    public InventoryDtos.CountResult count(@PathVariable Long productId, @RequestBody CountRequest request, Authentication auth) {
        return inventory.recordCount(productId, request.countedQuantity(), request.note(), auth.getName());
    }

    @PostMapping("/products/{productId}/incoming")
    @PreAuthorize(OWNER_OF_PRODUCT)
    public InventoryDtos.IncomingResponse createIncoming(@PathVariable Long productId, @RequestBody IncomingRequest request, Authentication auth) {
        return inventory.toResponse(inventory.createIncoming(productId, request.quantity(), request.unitCost(),
                request.supplier(), request.note(), auth.getName()));
    }

    // ---- Mzigo unaotarajiwa ----

    @PostMapping("/incoming/{incomingId}/receive")
    @PreAuthorize(OWNER_OF_INCOMING)
    public InventoryDtos.IncomingResponse receiveIncoming(@PathVariable Long incomingId, @RequestBody ReceiveIncomingRequest request, Authentication auth) {
        return inventory.toResponse(inventory.receiveIncoming(incomingId, request.receivedQuantity(), request.unitCost(), auth.getName()));
    }

    @PostMapping("/incoming/{incomingId}/cancel")
    @PreAuthorize(OWNER_OF_INCOMING)
    public InventoryDtos.IncomingResponse cancelIncoming(@PathVariable Long incomingId, @RequestBody ReasonRequest request, Authentication auth) {
        return inventory.toResponse(inventory.cancelIncoming(incomingId, request.reason(), auth.getName()));
    }

    // Rudisha data inayohitajika tu; usifichue object graph ya Product/Store/User.
    private InventoryMovementResponse toResponse(InventoryMovement m) {
        return new InventoryMovementResponse(m.getId(), m.getProduct().getId(), m.getProduct().getName(),
                m.getMovementType(), m.getQuantityChange(), m.getQuantityBefore(), m.getQuantityAfter(),
                m.getQuantity(), m.getUnitCost(), m.getReference(), m.getReason(), m.getActorEmail(), m.getCreatedAt());
    }

    public record ReceiveRequest(int quantity, BigDecimal unitCost, String reason) {}
    public record AdjustRequest(int delta, String reason) {}
    public record WriteOffRequest(String type, int quantity, String reason) {}
    public record CountRequest(int countedQuantity, String note) {}
    public record IncomingRequest(int quantity, BigDecimal unitCost, String supplier, String note) {}
    public record ReceiveIncomingRequest(int receivedQuantity, BigDecimal unitCost) {}
    public record ReasonRequest(String reason) {}
}
