package com.smart.instapreneur.Service;

import com.smart.instapreneur.Model.Cart;
import com.smart.instapreneur.Model.CartItem;
import com.smart.instapreneur.Model.Orders;
import com.smart.instapreneur.Model.OrderItems;
import com.smart.instapreneur.Model.Products;
import com.smart.instapreneur.Repository.CartItemRepository;
import com.smart.instapreneur.Repository.CartRepository;
import com.smart.instapreneur.Repository.OrdersRepository;
import com.smart.instapreneur.Repository.OrderItemsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public OrderServiceImpl(
            OrdersRepository ordersRepository,
            OrderItemsRepository orderItemsRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository) {

        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // CREATION OF AN ORDER
   
    @Override
    @Transactional
    public Orders createOrder(Long cartId, String deliveryAddress, String customerEmail) {
        // Load the cart from the database; a client-supplied ID is not proof of ownership.
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new com.smart.instapreneur.Exception.ResourceNotFoundException("kikapu hakipo"));
        if (cart.getCustomer() == null || cart.getCustomer().getUser() == null
                || !customerEmail.equalsIgnoreCase(cart.getCustomer().getUser().getEmail())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Huna ruhusa ya kuweka oda kutoka cart hii.");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new com.smart.instapreneur.Exception.IllegalArgumentException("Jaza mahali unapokaa au sehemu uliokaribu nayo");
        }
        List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);
        if (cartItems.isEmpty()) {
            throw new com.smart.instapreneur.Exception.IllegalArgumentException("Huwezi kuweka oda ikiwa kikapu chako ni tupu");
        }
        Products firstProduct = cartItems.get(0).getProducts();
        
        boolean containsAnotherStore = cartItems.stream().anyMatch(item ->
                item.getProducts().getStore() == null
                || !item.getProducts().getStore().getId().equals(firstProduct.getStore().getId()));
        if (containsAnotherStore) {
            throw new com.smart.instapreneur.Exception.IllegalArgumentException(
                    "Cart ina bidhaa kutoka maduka tofauti. Weka oda tofauti kwa kila duka.");
        }
        Orders order = new Orders();
        order.setCustomer(cart.getCustomer());
        order.setStore(firstProduct.getStore());
        order.setDeliveryAddress(deliveryAddress.trim());
        // Stock HAISHIKILIWI hapa; inashikiliwa mmiliki anapoidhinisha (OrderWorkflowService.approve).
        order.setStatus(OrderStatus.NEW);
        order.setStockReserved(false);
        order.setOrderNumber(OrderIdentifiers.newOrderNumber());
        order.setTrackingToken(OrderIdentifiers.newTrackingToken());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean needsReview = false;
        // Validate stock and calculate prices from database values, not frontend totals.
        for (CartItem cartItem : cartItems) {
            Products product = cartItem.getProducts();
            int quantity = cartItem.getQuantity();
            if (quantity <= 0) {
                throw new com.smart.instapreneur.Exception.IllegalArgumentException(
                        "Kiasi si sahihi kwa bidhaa: " + product.getName());
            }
            // Stock isipotosha, oda inaenda kwenye ukaguzi badala ya kukataliwa.
            if (product.getStockQuantity() < quantity) {
                needsReview = true;
            }
            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new com.smart.instapreneur.Exception.IllegalArgumentException(
                        "Bei ya bidhaa si sahihi: " + product.getName());
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        if (needsReview) {
            order.setStatus(OrderStatus.STOCK_REVIEW);
        }
        order.setTotalAmount(totalAmount);
        // Save the order first so every OrderItems row can reference a real order ID.
        Orders savedOrder = ordersRepository.save(order);
        List<OrderItems> createdOrderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Products product = cartItem.getProducts();
            OrderItems orderItem = new OrderItems();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSubTotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            createdOrderItems.add(orderItemsRepository.save(orderItem));
        }
        // Populate the response object too; the owning side was already saved above.
        savedOrder.setOrderItems(createdOrderItems);

        // Delete the actual CartItem rows (not the cart itself) after checkout succeeds.
        cartItemRepository.deleteAll(cartItems);
        return savedOrder;
    }
    // =========================================================
    // GET ORDER BY ID
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public Orders getOrderById(Long orderId) {
        return ordersRepository.findById(orderId)
                .orElseThrow(() ->
                        new com.smart.instapreneur.Exception.ResourceNotFoundException("Oda haikupatikana.")
                );
    }
    // =========================================================
    // GET ORDER BY ORDER NUMBER
    // =========================================================
    @Override
    public Orders getOrderByOrderNumber(
            String orderNumber) {
        return ordersRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() ->
                        new com.smart.instapreneur.Exception.ResourceNotFoundException("Oda haikupatikana.")
                );
    }
    // =========================================================
    // GET ORDERS BY STORE
    // =========================================================
    @Override
    public List<Orders> getOrdersByStore(Long storeId) {
        return ordersRepository
                .findByStoreId(storeId);
    }
    // =========================================================
    // GET ORDERS BY CUSTOMER
    // =========================================================
    @Override
    public List<Orders> getOrdersByCustomer(
            Long customerId) {
        return ordersRepository
                .findByCustomerId(customerId);
    }
}
