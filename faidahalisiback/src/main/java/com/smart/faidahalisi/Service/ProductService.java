package com.smart.faidahalisi.Service;

import java.util.List;

import com.smart.faidahalisi.Model.Products;

public interface ProductService {

    // Returns every product in the database.
    List<Products> getAllProducts();

    // Finds one product by id.
    Products getProductById(Long id);

    // Finds one product by name.
    Products getProductByName(String name);

    // Returns all products that belong to a store.
    List<Products> getProductsByStoreId(Long storeId);

    // Returns products from a store by status, for example ACTIVE or INACTIVE.
    List<Products> getProductsByStoreIdAndStatus(Long storeId, String status);

    // Creates a product and connects it to an existing store. Stock ya mwanzo inaandikwa kwenye historia.
    Products createProduct(Long storeId, Products product, String actor);

    // Updates product details. HAIBADILISHI stock: tumia InventoryService (mapokezi, marekebisho, hesabu).
    Products updateProduct(Long id, Products product, String actor);

    // Deletes a product, or hides it (INACTIVE) if it already appears in orders.
    void deleteProduct(Long id, String actor);
}
