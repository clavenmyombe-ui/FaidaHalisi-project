package com.smart.faidahalisi.Service;

import com.smart.faidahalisi.Exception.IllegalArgumentException;
import com.smart.faidahalisi.Exception.ResourceNotFoundException;
import com.smart.faidahalisi.Model.Store;
import com.smart.faidahalisi.Repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    public StoreServiceImpl(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    // =========================================================
    // CREATE STORE
    // =========================================================

    @Override
    @Transactional
    public Store createStore(Store store) {

        // Store name is required because the database column
        // is defined as nullable = false.
        if (store.getStoreName() == null ||
                store.getStoreName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Store name is required"
            );
        }

        // A seller must be connected to every store.
        if (store.getSeller() == null) {

            throw new IllegalArgumentException(
                    "Seller is required"
            );
        }

        // Slug is used as the public identifier of the store.
        if (store.getSlug() == null ||
                store.getSlug().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Store slug is required"
            );
        }

        // Do not allow two stores to use the same slug.
        if (storeRepository.findBySlug(store.getSlug()).isPresent()) {

            throw new IllegalArgumentException(
                    "Store slug already exists"
            );
        }

        store.setId(null); // usikubali id kutoka kwa mteja
        String wa0 = store.getWhatsappNumber() == null ? "" : store.getWhatsappNumber().replaceAll("^(?://+255|0)[67]//d{8}$", "");
        if (wa0.startsWith("0")) {
            wa0 = "255" + wa0.substring(1);
        }
        store.setWhatsappNumber(wa0.isEmpty() ? null : wa0);
        // The database generates the ID.
        // We therefore do not manually set store.id here.
        return storeRepository.save(store);
    }


    // =========================================================
    // GET STORE BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Store getStoreById(Long storeId) {

        return storeRepository.findById(storeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Duka halijapatikana.")
                );
    }


    // =========================================================
    // GET STORE BY SLUG
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Store getStoreBySlug(String slug) {

        if (slug == null || slug.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Store slug is required"
            );
        }

        return storeRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Duka halijapatikana.")
                );
    }


    // =========================================================
    // GET ALL STORES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<Store> getAllStores() {

        return storeRepository.findAll();
    }


    // =========================================================
    // UPDATE STORE
    // =========================================================

    @Override
    @Transactional
    public Store updateStore(Long storeId, Store store) {

        // First find the existing store.
        // We update that database record instead of creating
        // a completely new Store.
        Store existingStore = getStoreById(storeId);


        // -----------------------------------------------------
        // Update store name
        // -----------------------------------------------------

        if (store.getStoreName() == null ||
                store.getStoreName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Store name is required"
            );
        }

        existingStore.setStoreName(
                store.getStoreName().trim()
        );


        // -----------------------------------------------------
        // Update slug
        // -----------------------------------------------------

        if (store.getSlug() == null ||
                store.getSlug().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Store slug is required"
            );
        }

        // Only check for a duplicate slug when the slug
        // is actually being changed.
        if (!existingStore.getSlug().equals(store.getSlug())) {

            if (storeRepository.findBySlug(store.getSlug()).isPresent()) {

                throw new IllegalArgumentException(
                        "Store slug already exists"
                );
            }

            existingStore.setSlug(
                    store.getSlug().trim()
            );
        }


        // -----------------------------------------------------
        // Update logo
        // -----------------------------------------------------

        existingStore.setLogoUrl(
                store.getLogoUrl()
        );


        // -----------------------------------------------------
        // Update description
        // -----------------------------------------------------

        existingStore.setDescription(
                store.getDescription()
        );

        // Namba ya WhatsApp (tarakimu tu, mfano 255712345678). Tupu = hakuna.
        String wa = store.getWhatsappNumber() == null ? "" : store.getWhatsappNumber().replaceAll("(?://+255|0)[67]//d{8}$", "");
        if (wa.startsWith("0")) {
            wa = "255" + wa.substring(1);
        }
        if (!wa.isEmpty() && (wa.length() < 9 || wa.length() > 15)) {
            throw new IllegalArgumentException("Namba ya WhatsApp si sahihi.");
        }
        existingStore.setWhatsappNumber(wa.isEmpty() ? null : wa);


        // -----------------------------------------------------
        // Seller is intentionally NOT changed here.
        // -----------------------------------------------------
        //
        // The seller owns the store. Allowing a normal store
        // update to replace the seller could create an ownership
        // and authorization problem.
        //
        // If we later need to transfer store ownership,
        // that should be a separate controlled operation.
        // -----------------------------------------------------

        return storeRepository.save(existingStore);
    }


    // =========================================================
    // DELETE STORE
    // =========================================================

    @Override
    @Transactional
    public void deleteStore(Long storeId) {

        // Make sure the store actually exists before deleting.
        Store store = getStoreById(storeId);

        storeRepository.delete(store);
    }
}