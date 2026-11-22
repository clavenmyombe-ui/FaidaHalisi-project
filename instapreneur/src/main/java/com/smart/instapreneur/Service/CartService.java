package com.smart.instapreneur.Service;

import java.util.List;

import com.smart.instapreneur.Model.Cart;
import com.smart.instapreneur.Model.CartItem;

public interface CartService {

    // Creates a cart for a customer if they do not already have one.
    Cart createCart(Long customerId);

    // Finds a cart by its cart id.
    Cart getCartById(Long cartId);

    // Finds the cart that belongs to a specific customer.
    Cart getCartByCustomerId(Long customerId);

    // Returns all items inside a cart.
    List<CartItem> getCartItems(Long cartId);

    // Removes all items from a cart but keeps the cart itself.
    void clearCart(Long cartId);

    // Deletes the cart completely.
    void deleteCart(Long cartId);
}
