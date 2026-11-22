package com.smart.faidahalisi.Service;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.smart.faidahalisi.Exception.IllegalArgumentException;
import com.smart.faidahalisi.Exception.ResourceNotFoundException;
import com.smart.faidahalisi.Model.Cart;
import com.smart.faidahalisi.Model.CartItem;
import com.smart.faidahalisi.Model.Products;
import com.smart.faidahalisi.Repository.CartItemRepository;
import com.smart.faidahalisi.Repository.CartRepository;
import com.smart.faidahalisi.Repository.ProductRepository;

@Service
public class CartItemServiceImpl implements CartItemService {
    // These repositories allow the service to work with cart items, carts, and products.
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    // Constructor injection is used so Spring can provide the required repositories.
    public CartItemServiceImpl(CartItemRepository cartItemRepository, CartRepository cartRepository,
            ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    // Adds a product to a cart.
    // If the product is already in the cart, this method increases its quantity.
    @Override
    public CartItem addItem(Long cartId, Long productId, int quantity) {
        validateId(cartId, "Cart id is required");
        validateId(productId, "Product id is required");
        validateQuantity(quantity);

        // The cart must exist before an item can be added to it.
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        // The product must exist before it can be added to the cart.
        Products products = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Kikapu ni ombi la mteja, si uthibitisho wa stock wala reservation.
        // Stock itakaguliwa tena wakati oda inapowasilishwa/kuthibitishwa.
        // Check whether this product is already inside this cart.
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductsId(cartId, productId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            // Ruhusu kiasi kinachozidi stock ya sasa; kampuni itathibitisha upatikanaji.
            item.setQuantity(newQuantity);
            return cartItemRepository.save(item);
        }

        // If the product is not already in the cart, create a new cart item.
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProducts(products);
        item.setQuantity(quantity);

        return cartItemRepository.save(item);
    }

    // Load an item before editing it so the controller can verify cart ownership.
    @Override
    public CartItem getCartItemById(Long cartItemId) {
        validateId(cartItemId, "Cart item id is required");
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
    }

    // Removes one cart item completely from the cart.
    @Override
    public void removeItem(Long cartItemId) {
        validateId(cartItemId, "Cart item id is required");

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        cartItemRepository.delete(item);
    }

    // Decreases the quantity of one cart item by one.
    // Quantity is not allowed to go below one.
    @Override
    public CartItem decreaseQuantity(Long cartItemId) {
        validateId(cartItemId, "Cart item id is required");

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (item.getQuantity() <= 1) {
            throw new IllegalArgumentException("Quantity cannot be less than 1");
        }

        item.setQuantity(item.getQuantity() - 1);
        return cartItemRepository.save(item);
    }

    // Common id validation used by all public methods in this service.
    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // Cart item quantity must be a positive whole number.
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Kiasi lazima kiwe namba nzima kuanzia 1");
        }
    }
}
