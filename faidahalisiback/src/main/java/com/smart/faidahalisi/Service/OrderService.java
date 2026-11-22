package com.smart.faidahalisi.Service;

import com.smart.faidahalisi.Model.Orders;

import java.util.List;

/**
 * Kusoma na kuunda oda. Kubadilisha hali ya oda (idhinisha, ghairi, kufikisha...) kunafanywa
 * PEKEE na OrderWorkflowService.
 */
public interface OrderService {

    // Create an order from a customer's cart
    Orders createOrder(Long cartId, String deliveryAddress, String customerEmail);

    // Find an order using its database ID
    Orders getOrderById(Long orderId);

    // Find an order using its customer-facing order number
    Orders getOrderByOrderNumber(String orderNumber);

    // Get all orders belonging to a particular store
    List<Orders> getOrdersByStore(Long storeId);

    // Get all orders belonging to a particular customer
    List<Orders> getOrdersByCustomer(Long customerId);
}
