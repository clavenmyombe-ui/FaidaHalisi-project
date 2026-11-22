package com.smart.faidahalisi.Service;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smart.faidahalisi.Exception.IllegalArgumentException;
import com.smart.faidahalisi.Exception.ResourceNotFoundException;
import com.smart.faidahalisi.Model.Cart;
import com.smart.faidahalisi.Model.CartItem;
import com.smart.faidahalisi.Model.Customer;
import com.smart.faidahalisi.Repository.CartItemRepository;
import com.smart.faidahalisi.Repository.CartRepository;
import com.smart.faidahalisi.Repository.CustomerRepository;

@Service
public class CartServiceImpl implements CartService {
    // These repositories allow CartService to read and save cart-related data.
    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;
    private final CartItemRepository cartItemRepository;

    // Constructor injection is used so Spring can provide the required repositories.
    public CartServiceImpl(CartRepository cartRepository, CustomerRepository customerRepository,
            CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.customerRepository = customerRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // Creates a cart for a customer if they do not already have one.
    // If the customer already has a cart, the existing cart is returned.
    @Override
    public Cart createCart(Long customerId) {
        validateId(customerId, "Customer id is required");

        // A customer should use one active cart, so we check for an existing cart first.
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    // A cart cannot exist without a valid customer.
                    Customer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
                    // Connect the new cart to the customer before saving it.
                    Cart cart = new Cart();
                    cart.setCustomer(customer);

                    return cartRepository.save(cart);
                });
    }

    // Finds a cart by its cart id.
    // This is useful before reading, clearing, or deleting a cart.
    @Override
    public Cart getCartById(Long cartId) {
        validateId(cartId, "Cart id is required");

        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));
    }

    // Finds the cart that belongs to a specific customer.
    @Override
    public Cart getCartByCustomerId(Long customerId) {
        validateId(customerId, "Customer id is required");

        // This gives a clearer error if the customer id itself is wrong.
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer id: " + customerId));
    }

    // Returns all items inside a cart.
    @Override
    public List<CartItem> getCartItems(Long cartId) {
        // First confirm that the cart exists.
        getCartById(cartId);
        return cartItemRepository.findByCartId(cartId);
    }

    // Removes all items from a cart but keeps the cart itself.
    // This is useful after checkout or when a customer empties their cart.
    @Override
    @Transactional
    public void clearCart(Long cartId) {
        // First confirm that the cart exists.
        getCartById(cartId);

        // Delete all cart items connected to this cart.
        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        cartItemRepository.deleteAll(items);
    }

    // Deletes the cart completely.
    // Items are deleted first to avoid leaving cart items without a cart.
    @Override
    @Transactional
    public void deleteCart(Long cartId) {
        Cart cart = getCartById(cartId);

        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        cartItemRepository.deleteAll(items);

        cartRepository.delete(cart);
    }

    // Common id validation used by all public methods in this service.
    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
