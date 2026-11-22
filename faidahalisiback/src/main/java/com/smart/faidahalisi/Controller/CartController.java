package com.smart.faidahalisi.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.smart.faidahalisi.Model.Cart;
import com.smart.faidahalisi.Model.CartItem;
import com.smart.faidahalisi.Model.Customer;
import com.smart.faidahalisi.Service.CartService;
import com.smart.faidahalisi.Service.CustomerService;
import com.smart.faidahalisi.Repository.CartRepository;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    private final CartService cartService;
    private final CustomerService customerService;
    private final CartRepository cartRepository;
    private final com.smart.faidahalisi.Service.CartItemService cartItemService;

    public CartController(CartService cartService, CustomerService customerService, CartRepository cartRepository,
            com.smart.faidahalisi.Service.CartItemService cartItemService) {
        this.cartService = cartService;
        this.customerService = customerService;
        this.cartRepository = cartRepository;
        this.cartItemService = cartItemService;
    }

    // Guest carts use an unguessable UUID capability; only its holder can read or change its rows.
    @GetMapping("/guest/{key}/items")
    public ResponseEntity<List<CartItem>> getGuestItems(@PathVariable String key) {
        Cart cart = guestCart(key, false);
        return ResponseEntity.ok(cart == null ? List.of() : cartService.getCartItems(cart.getId()));
    }

    @PostMapping("/guest/{key}/items")
    public ResponseEntity<CartItem> addGuestItem(@PathVariable String key, @RequestBody GuestItemRequest request) {
        Cart cart = guestCart(key, true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartItemService.addItem(cart.getId(), request.productId(), request.quantity()));
    }

    @DeleteMapping("/guest/{key}/items/{itemId}")
    public ResponseEntity<Void> removeGuestItem(@PathVariable String key, @PathVariable Long itemId) {
        Cart cart = guestCart(key, false);
        CartItem item = cartItemService.getCartItemById(itemId);
        if (cart == null || !item.getCart().getId().equals(cart.getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        cartItemService.removeItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/guest/{key}/items")
    public ResponseEntity<Void> clearGuestItems(@PathVariable String key) {
        Cart cart = guestCart(key, false);
        if (cart != null) cartService.clearCart(cart.getId());
        return ResponseEntity.noContent().build();
    }

    private Cart guestCart(String key, boolean create) {
        try { UUID.fromString(key); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cart key"); }
        return cartRepository.findByGuestKey(key).orElseGet(() -> {
            if (!create) return null;
            Cart cart = new Cart(); cart.setGuestKey(key); return cartRepository.save(cart);
        });
    }

    public record GuestItemRequest(Long productId, int quantity) {}

    // POST /api/carts/me creates a cart if needed, otherwise returns the existing cart.
    // POST /api/carts/me huunda cart ikiwa haipo; Authentication hutokana na JWT.
    @PostMapping("/me")
    public ResponseEntity<Cart> getOrCreateMyCart(Authentication authentication) {
        Customer customer = customerService.getMyCustomer(authentication.getName());
        return ResponseEntity.ok(cartService.createCart(customer.getId()));
    }

    // GET /api/carts/me returns only the cart connected to the logged-in customer's profile.
    // GET /api/carts/me hurudisha cart ya customer aliyeingia pekee.
    @GetMapping("/me")
    public ResponseEntity<Cart> getMyCart(Authentication authentication) {
        Customer customer = customerService.getMyCustomer(authentication.getName());
        return ResponseEntity.ok(cartService.getCartByCustomerId(customer.getId()));
    }

    // GET /api/carts/me/items hurudisha bidhaa zote ndani ya cart ya akaunti hii.
    @GetMapping("/me/items")
    public ResponseEntity<List<CartItem>> getMyCartItems(Authentication authentication) {
        Customer customer = customerService.getMyCustomer(authentication.getName());
        Cart cart = cartService.getCartByCustomerId(customer.getId());
        return ResponseEntity.ok(cartService.getCartItems(cart.getId()));
    }

    // DELETE /api/carts/me/items hufuta mistari ya bidhaa lakini huacha cart yenyewe.
    @DeleteMapping("/me/items")
    public ResponseEntity<Void> clearMyCart(Authentication authentication) {
        Customer customer = customerService.getMyCustomer(authentication.getName());
        Cart cart = cartService.getCartByCustomerId(customer.getId());
        cartService.clearCart(cart.getId());
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/carts/me hufuta cart nzima ya akaunti iliyothibitishwa.
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyCart(Authentication authentication) {
        Customer customer = customerService.getMyCustomer(authentication.getName());
        Cart cart = cartService.getCartByCustomerId(customer.getId());
        cartService.deleteCart(cart.getId());
        return ResponseEntity.noContent().build();
    }
}
