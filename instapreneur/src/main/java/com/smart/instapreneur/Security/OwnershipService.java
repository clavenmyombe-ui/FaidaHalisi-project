package com.smart.instapreneur.Security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.smart.instapreneur.Repository.DiscrepancyRepository;
import com.smart.instapreneur.Repository.IncomingStockRepository;
import com.smart.instapreneur.Repository.OrdersRepository;
import com.smart.instapreneur.Repository.ProductRepository;
import com.smart.instapreneur.Repository.StoreRepository;

/**
 * Ukaguzi wa umiliki unaotumika na @PreAuthorize kabla endpoint haijafanya kazi.
 * Hii huzuia mtumiaji kubadilisha ID kwenye URL na kufikia data ya mtu mwingine.
 */
@Component("ownership")
public class OwnershipService {
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrdersRepository ordersRepository;
    private final IncomingStockRepository incomingRepository;
    private final DiscrepancyRepository discrepancyRepository;

    public OwnershipService(StoreRepository storeRepository, ProductRepository productRepository,
            OrdersRepository ordersRepository, IncomingStockRepository incomingRepository,
            DiscrepancyRepository discrepancyRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.ordersRepository = ordersRepository;
        this.incomingRepository = incomingRepository;
        this.discrepancyRepository = discrepancyRepository;
    }

    /** Mzigo unaotarajiwa ni wa bidhaa ya duka la aliyeingia? */
    public boolean isIncomingOwner(Long incomingId, Authentication auth) {
        if (!isAuthenticated(auth)) return false;
        return incomingRepository.findById(incomingId)
                .map(in -> in.getProduct().getStore().getSeller() != null
                        && auth.getName().equalsIgnoreCase(in.getProduct().getStore().getSeller().getEmail()))
                .orElse(false);
    }

    /** Kesi ya tofauti ni ya duka la aliyeingia? */
    public boolean isDiscrepancyOwner(Long discrepancyId, Authentication auth) {
        if (!isAuthenticated(auth)) return false;
        return discrepancyRepository.findById(discrepancyId)
                .map(d -> isStoreOwner(d.getStoreId(), auth))
                .orElse(false);
    }

    public boolean isStoreOwner(Long storeId, Authentication auth) {
        if (!isAuthenticated(auth)) return false;
        return storeRepository.findById(storeId)
                .map(store -> store.getSeller() != null && auth.getName().equalsIgnoreCase(store.getSeller().getEmail()))
                .orElse(false);
    }

    public boolean isProductOwner(Long productId, Authentication auth) {
        if (!isAuthenticated(auth)) return false;
        return productRepository.findById(productId)
                .map(product -> product.getStore() != null && product.getStore().getSeller() != null
                        && auth.getName().equalsIgnoreCase(product.getStore().getSeller().getEmail()))
                .orElse(false);
    }

    public boolean isOrderOwner(Long orderId, Authentication auth) {
        if (!isAuthenticated(auth)) return false;
        return ordersRepository.findById(orderId)
                .map(order -> order.getStore() != null && order.getStore().getSeller() != null
                        && auth.getName().equalsIgnoreCase(order.getStore().getSeller().getEmail()))
                .orElse(false);
    }

    public boolean isOrderNumberOwner(String orderNumber, Authentication auth) {
        if (!isAuthenticated(auth)) return false;
        return ordersRepository.findByOrderNumber(orderNumber)
                .map(order -> order.getStore() != null && order.getStore().getSeller() != null
                        && auth.getName().equalsIgnoreCase(order.getStore().getSeller().getEmail()))
                .orElse(false);
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getName() != null;
    }
}