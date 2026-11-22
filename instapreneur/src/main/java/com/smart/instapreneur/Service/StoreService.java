package com.smart.instapreneur.Service;

import com.smart.instapreneur.Model.Store;

import java.util.List;

public interface StoreService {

    // Create a new store.
    Store createStore(Store store);

    // Get one store using its database ID.
    Store getStoreById(Long storeId);

    // Get one store using its public slug.
    Store getStoreBySlug(String slug);

    // Get all stores.
    List<Store> getAllStores();

    // Update an existing store.
    Store updateStore(Long storeId, Store store);

    // Delete a store.
    void deleteStore(Long storeId);
}