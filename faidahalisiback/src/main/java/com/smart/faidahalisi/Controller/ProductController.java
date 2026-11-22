package com.smart.faidahalisi.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.faidahalisi.Model.Products;
import com.smart.faidahalisi.Service.InventoryService;
import com.smart.faidahalisi.Service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    // Spring injects the services so the controller can delegate business operations.
    public ProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    // Bidhaa kamili (zenye stock na gharama) ni za ndani tu. Wateja wanatumia /api/catalog/products.
    // GET /api/products: ADMIN pekee.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Products>> getAllProducts() {
        List<Products> products =
                productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
 
    // GET /api/products/{id} hutafuta bidhaa moja kwa ID yake.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isProductOwner(#p0, authentication)")
    public ResponseEntity<Products> getProductById(
            @PathVariable Long id) {

        Products product =
                productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    // GET /api/products/store/{storeId} hurudisha bidhaa zote za duka lililotajwa.
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isStoreOwner(#p0, authentication)")
    public ResponseEntity<List<Products>> getProductsByStoreId(
            @PathVariable Long storeId) {
        List<Products> products =
                productService.getProductsByStoreId(storeId);
        return ResponseEntity.ok(products);
    }

    // GET /api/products/store/{storeId}/status/{status} huchuja bidhaa kwa duka na hali.
    @GetMapping("/store/{storeId}/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or @ownership.isStoreOwner(#p0, authentication)")
    public ResponseEntity<List<Products>> getProductsByStoreIdAndStatus(
            @PathVariable Long storeId,
            @PathVariable String status) {

        List<Products> products =
                productService.getProductsByStoreIdAndStatus(
                        storeId,
                        status
                );
        return ResponseEntity.ok(products);
    }
  
    // POST /api/products/store/{storeId} huunda bidhaa; PreAuthorize huthibitisha mmiliki wa duka.
    @PostMapping("/store/{storeId}")
    @PreAuthorize("hasRole('SELLER') and @ownership.isStoreOwner(#p0, authentication)")
    public ResponseEntity<Products> createProduct(
            @PathVariable Long storeId,
            @RequestBody Products product, Authentication authentication) {
    
        Products createdProduct =
                productService.createProduct(storeId, product, authentication.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProduct);
    }

    // PUT /api/products/{id} husasisha bidhaa nzima kwa mmiliki wa bidhaa pekee.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') and @ownership.isProductOwner(#p0, authentication)")
    public ResponseEntity<Products> updateProduct(
            @PathVariable Long id,
            @RequestBody Products product, Authentication authentication) {
        Products updatedProduct =
                productService.updateProduct(id, product, authentication.getName());
        return ResponseEntity.ok(updatedProduct);
    }
 
    // PUT /api/products/{id}/stock?stockQuantity=N&reason=...
    // Njia ya zamani ya kuweka stock. Sasa inapita InventoryService: sababu ni lazima na historia inaandikwa.
    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('SELLER') and @ownership.isProductOwner(#p0, authentication)")
    public ResponseEntity<Products> updateStockQuantity(
            @PathVariable Long id,
            @RequestParam int stockQuantity,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        inventoryService.adjustTo(id, stockQuantity, reason, authentication.getName());
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // DELETE /api/products/{id} hufuta bidhaa na hurudisha 204 bila body.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') and @ownership.isProductOwner(#p0, authentication)")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id, Authentication authentication) {
        productService.deleteProduct(id, authentication.getName());
        // 204 = request succeeded and there is no response body.
        return ResponseEntity.noContent().build();
    }
}
