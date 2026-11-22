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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.faidahalisi.Model.Store;
import com.smart.faidahalisi.Service.StoreService;
import com.smart.faidahalisi.Repository.UserRepository;
import com.smart.faidahalisi.Model.User;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;

    // Constructor injection allows Spring to provide the StoreService.
    public StoreController(StoreService storeService, UserRepository userRepository) {
        this.storeService = storeService;
        this.userRepository = userRepository;
    }
    // CREATE STORE
    // =========================================================
    // POST /api/stores huunda duka na kuunganisha muuzaji kutoka JWT, si body ya Angular.
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Store> createStore(
            @RequestBody Store store, Authentication authentication) {
        // Business validation and database operations are handled
        // by StoreService. The controller only handles the HTTP request.
        User seller = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Akaunti haijapatikana."));
        // Never trust sellerId/seller from the request body.
        store.setSeller(seller);
        Store createdStore = storeService.createStore(store);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdStore);
    }
    // Duka la muuzaji aliyeingia (frontend inalitumia kutambua duka lake bila kutegemea email ya umma).
    // GET /api/stores/mine
    @GetMapping("/mine")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Store> myStore(Authentication authentication) {
        return storeService.getAllStores().stream()
                .filter(s -> s.getSeller() != null && authentication.getName().equalsIgnoreCase(s.getSeller().getEmail()))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
    // GET STORE BY ID
    // =========================================================
    // GET /api/stores/{storeId} husoma duka kwa kitambulisho chake.
    @GetMapping("/{storeId}")
    public ResponseEntity<Store> getStoreById(
            @PathVariable Long storeId) {
        Store store = storeService.getStoreById(storeId);
        return ResponseEntity.ok(store);
    }
    // GET STORE BY SLUG
    // GET /api/stores/slug/{slug} hutafuta duka kupitia slug ya kiungo cha umma.
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Store> getStoreBySlug(
            @PathVariable String slug) {
        Store store = storeService.getStoreBySlug(slug);
        return ResponseEntity.ok(store);
    }
    // GET ALL STORES
    // =========================================================
    // GET /api/stores/store hurudisha orodha ya maduka yote.
    @GetMapping("/store")
    public ResponseEntity<List<Store>> getAllStores() {
        List<Store> stores = storeService.getAllStores();
        return ResponseEntity.ok(stores);
    }
    //     // UPDATE STORE
    // =========================================================
    // PUT /api/stores/{storeId} husasisha duka ikiwa caller ndiye mmiliki wake.
    @PutMapping("/{storeId}")
    @PreAuthorize("hasRole('SELLER') and @ownership.isStoreOwner(#p0, authentication)")
    public ResponseEntity<Store> updateStore(
            @PathVariable Long storeId,
            @RequestBody Store store) {
        Store updatedStore =
                storeService.updateStore(storeId, store);
        return ResponseEntity.ok(updatedStore);
    }
    // DELETE STORE
    // =========================================================
    // DELETE /api/stores/{storeId} hufuta duka kwa mmiliki au role iliyoruhusiwa.
    @DeleteMapping("/{storeId}")
    @PreAuthorize("hasRole('SELLER') and @ownership.isStoreOwner(#p0, authentication)")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long storeId) {
        storeService.deleteStore(storeId);
        // 204 means the operation succeeded but there is
        // no response body to return.
        return ResponseEntity.noContent().build();
    }
}
