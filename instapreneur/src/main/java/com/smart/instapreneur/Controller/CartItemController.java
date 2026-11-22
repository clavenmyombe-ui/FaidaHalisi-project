package com.smart.instapreneur.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.smart.instapreneur.Model.Cart;
import com.smart.instapreneur.Model.CartItem;
import com.smart.instapreneur.Service.CartItemService;
import com.smart.instapreneur.Service.CartService;

@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {
    private final CartItemService cartItemService;
    private final CartService cartService;

    public CartItemController(CartItemService cartItemService, CartService cartService) {
        this.cartItemService = cartItemService;
        this.cartService = cartService;
    }

    // The cart ID is not enough to grant access; verify that it belongs to this account first.
    // POST /api/cart-items/cart/{cartId}/product/{productId}?quantity=N huongeza bidhaa baada ya kuthibitisha umiliki.
    @PostMapping("/cart/{cartId}/product/{productId}")
    public ResponseEntity<CartItem> addItem(@PathVariable Long cartId, @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int quantity, Authentication authentication) {
        assertCartOwner(cartId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartItemService.addItem(cartId, productId, quantity));
    }

    // DELETE /api/cart-items/{cartItemId} huondoa mstari mmoja ikiwa cart ni ya mtumiaji huyu.
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long cartItemId, Authentication authentication) {
        assertItemOwner(cartItemId, authentication.getName());
        cartItemService.removeItem(cartItemId);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/cart-items/{cartItemId}/decrease hupunguza quantity kwa moja baada ya ukaguzi wa umiliki.
    @PutMapping("/{cartItemId}/decrease")
    public ResponseEntity<CartItem> decreaseQuantity(@PathVariable Long cartItemId, Authentication authentication) {
        assertItemOwner(cartItemId, authentication.getName());
        return ResponseEntity.ok(cartItemService.decreaseQuantity(cartItemId));
    }

    private void assertCartOwner(Long cartId, String email) {
        Cart cart = cartService.getCartById(cartId);
        if (cart.getCustomer() == null || cart.getCustomer().getUser() == null
                || !email.equalsIgnoreCase(cart.getCustomer().getUser().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Huna ruhusa ya kutumia cart hii.");
        }
    }

    private void assertItemOwner(Long cartItemId, String email) {
        CartItem item = cartItemService.getCartItemById(cartItemId);
        assertCartOwner(item.getCart().getId(), email);
    }
}
