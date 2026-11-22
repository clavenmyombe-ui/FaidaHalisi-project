package com.smart.instapreneur.Service;

import com.smart.instapreneur.Model.CartItem;

public interface CartItemService {

    // Adds a product to a cart.
    CartItem addItem(Long cartId, Long productId, int quantity);

    // Load one item so the API can check that its cart belongs to the current customer.
    CartItem getCartItemById(Long cartItemId);

    // Removes one cart item completely from the cart.
    void removeItem(Long cartItemId);

    // Decreases the quantity of one cart item by one.
    CartItem decreaseQuantity(Long cartItemId);
}
