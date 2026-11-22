package com.smart.instapreneur.Security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.smart.instapreneur.Repository.OrdersRepository;

/** Checks whether an authenticated customer owns an order. */
@Component("customerAuthorization")
public class CustomerAuthorizationService {
    private final OrdersRepository ordersRepository;

    public CustomerAuthorizationService(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }

    public boolean isOrderOwner(Long orderId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return ordersRepository.findById(orderId)
                .map(order -> order.getCustomer() != null
                        && order.getCustomer().getUser() != null
                        && authentication.getName().equalsIgnoreCase(order.getCustomer().getUser().getEmail()))
                .orElse(false);
    }

    public boolean isOrderNumberOwner(String orderNumber, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return ordersRepository.findByOrderNumber(orderNumber)
                .map(order -> order.getCustomer() != null
                        && order.getCustomer().getUser() != null
                        && authentication.getName().equalsIgnoreCase(order.getCustomer().getUser().getEmail()))
                .orElse(false);
    }
}
