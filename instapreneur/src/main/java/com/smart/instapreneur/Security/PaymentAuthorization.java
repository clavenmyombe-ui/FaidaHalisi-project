package com.smart.instapreneur.Security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.smart.instapreneur.Model.Orders;
import com.smart.instapreneur.Repository.PaymentRepository;

/**
 * Inajibu swali moja: "Je, aliyeingia ndiye muuzaji mwenye duka lenye oda ya malipo haya?"
 * Inatumika ndani ya @PreAuthorize kama: @paymentAuthorization.isPaymentStoreOwner(#p0, authentication)
 */
@Component("paymentAuthorization")
public class PaymentAuthorization {

    private final PaymentRepository paymentRepository;

    public PaymentAuthorization(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // paymentId -> oda -> duka -> muuzaji -> email, kisha linganisha na aliyeingia.
    public boolean isPaymentStoreOwner(Long paymentId, Authentication authentication) {
        if (paymentId == null || authentication == null) {
            return false;
        }
        return paymentRepository.findById(paymentId)
                .map(payment -> isSeller(payment.getOrder(), authentication))
                .orElse(false);
    }

    private boolean isSeller(Orders order, Authentication authentication) {
        if (order == null || order.getStore() == null || order.getStore().getSeller() == null) {
            return false;
        }
        String sellerEmail = order.getStore().getSeller().getEmail();
        return sellerEmail != null && sellerEmail.equalsIgnoreCase(authentication.getName());
    }
}
