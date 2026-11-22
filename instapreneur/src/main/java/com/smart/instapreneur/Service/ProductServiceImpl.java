package com.smart.instapreneur.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Exception.IllegalArgumentException;
import com.smart.instapreneur.Exception.ResourceNotFoundException;
import com.smart.instapreneur.Model.Products;
import com.smart.instapreneur.Model.Store;
import com.smart.instapreneur.Repository.CartItemRepository;
import com.smart.instapreneur.Repository.IncomingStockRepository;
import com.smart.instapreneur.Repository.InventoryMovementRepository;
import com.smart.instapreneur.Repository.OrderItemsRepository;
import com.smart.instapreneur.Repository.OrderReturnRepository;
import com.smart.instapreneur.Repository.ProductRepository;
import com.smart.instapreneur.Repository.StoreRepository;

@Service
public class ProductServiceImpl implements ProductService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryService inventoryService;
    private final AuditService audit;
    private final InventoryMovementRepository movementRepository;
    private final IncomingStockRepository incomingRepository;
    private final OrderReturnRepository returnRepository;

    public ProductServiceImpl(ProductRepository productRepository, StoreRepository storeRepository,
            OrderItemsRepository orderItemsRepository, CartItemRepository cartItemRepository,
            InventoryService inventoryService, AuditService audit,
            InventoryMovementRepository movementRepository, IncomingStockRepository incomingRepository,
            OrderReturnRepository returnRepository) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.cartItemRepository = cartItemRepository;
        this.inventoryService = inventoryService;
        this.audit = audit;
        this.movementRepository = movementRepository;
        this.incomingRepository = incomingRepository;
        this.returnRepository = returnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Products> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Products getProductById(Long id) {
        validateId(id, "Product id is required");
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bidhaa haijapatikana."));
    }

    @Override
    @Transactional(readOnly = true)
    public Products getProductByName(String name) {
        validateText(name, "Product name is required");
        return productRepository.findByName(name.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Bidhaa haijapatikana."));
    }

    // Duka jipya halina bidhaa bado: hii si kosa, kwa hiyo tunarudisha orodha tupu (si 404).
    @Override
    @Transactional(readOnly = true)
    public List<Products> getProductsByStoreId(Long storeId) {
        validateId(storeId, "Store id is required");
        ensureStoreExists(storeId);
        return productRepository.findByStoreId(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Products> getProductsByStoreIdAndStatus(Long storeId, String status) {
        validateId(storeId, "Store id is required");
        validateText(status, "Product status is required");
        ensureStoreExists(storeId);
        return productRepository.findByStoreIdAndStatus(storeId, status.trim().toUpperCase());
    }

    @Override
    @Transactional
    public Products createProduct(Long storeId, Products product, String actor) {
        validateId(storeId, "Store id is required");
        validateProduct(product);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Duka halijapatikana."));

        // Usikubali id kutoka kwa mteja (ingeweza kubadilisha bidhaa ya mtu mwingine).
        product.setId(null);
        product.setOrderItems(null);
        product.setName(product.getName().trim());
        product.setStore(store);

        if (product.getStatus() == null || product.getStatus().isBlank()) {
            product.setStatus(STATUS_ACTIVE);
        } else {
            product.setStatus(product.getStatus().trim().toUpperCase());
        }
        if (product.getImageUrl() != null) {
            product.setImageUrl(product.getImageUrl().trim());
        }
        Products saved = productRepository.save(product);
        // Stock ya mwanzo inaingia kwenye historia ili idadi iwe na asili inayoeleweka.
        inventoryService.recordOpeningStock(saved, actor);
        audit.record(storeId, actor, "PRODUCT_CREATED", "PRODUCT", saved.getId(), null,
                saved.getName() + " @ " + saved.getPrice(), null);
        return saved;
    }

    @Override
    @Transactional
    public Products updateProduct(Long id, Products product, String actor) {
        validateId(id, "Product id is required");
        validateProduct(product);

        Products existing = getProductById(id);
        Long storeId = existing.getStore().getId();

        // Mabadiliko ya bei yanaandikwa kwenye audit: bei ya zamani na mpya.
        if (existing.getPrice().compareTo(product.getPrice()) != 0) {
            audit.record(storeId, actor, "PRICE_CHANGE", "PRODUCT", id,
                    existing.getPrice().toPlainString(), product.getPrice().toPlainString(), null);
        }
        // Gharama ya ununuzi: inabadilishwa tu ikiwa imetumwa, na inaandikwa kwenye audit.
        if (product.getCostPrice() != null && !Objects.equals(existing.getCostPrice(), product.getCostPrice())) {
            audit.record(storeId, actor, "COST_CHANGE", "PRODUCT", id,
                    String.valueOf(existing.getCostPrice()), product.getCostPrice().toPlainString(), "Imebadilishwa mkono");
            existing.setCostPrice(product.getCostPrice());
        }
        existing.setName(product.getName().trim());
        existing.setPrice(product.getPrice());
        existing.setImageUrl(product.getImageUrl() == null ? null : product.getImageUrl().trim());
        if (product.getCategory() != null && !product.getCategory().isBlank()) {
            existing.setCategory(product.getCategory().trim());
        }
        if (product.getStatus() != null && !product.getStatus().isBlank()) {
            existing.setStatus(product.getStatus().trim().toUpperCase());
        }
        // KUMBUKA: stockQuantity haibadilishwi hapa kwa makusudi. Mabadiliko ya stock yote yanapita
        // InventoryService ili yawe na sababu, historia na mhusika.
        return productRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, String actor) {
        Products product = getProductById(id);
        Long storeId = product.getStore().getId();
        // Bidhaa iliyowahi kuagizwa lazima ibaki kwa historia ya oda; tunaificha tu.
        // Bidhaa yenye historia (oda, cart, harakati za stock, mzigo, returns) haiwezi kufutwa kwa sababu ya foreign key;
        // tunaificha (INACTIVE) ili historia ibaki sahihi.
        if (orderItemsRepository.existsByProductId(id) || cartItemRepository.existsByProductsId(id)
                || movementRepository.existsByProductId(id) || incomingRepository.existsByProductId(id)
                || returnRepository.existsByProductId(id)) {
            product.setStatus(STATUS_INACTIVE);
            productRepository.save(product);
            audit.record(storeId, actor, "PRODUCT_HIDDEN", "PRODUCT", id, null, STATUS_INACTIVE,
                    "Imewahi kuagizwa; imefichwa badala ya kufutwa");
            return;
        }
        productRepository.delete(product);
        audit.record(storeId, actor, "PRODUCT_DELETED", "PRODUCT", id, product.getName(), null, null);
    }

    private void validateProduct(Products product) {
        if (product == null) {
            throw new IllegalArgumentException("Product details are required");
        }
        validateText(product.getName(), "Product name is required");
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("bei ya bidhaa lazima iwe kubwa kuliko sifuri");
        }
        if (product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("idadi ya bidhaa stoo haiwezi kuwa hasi");
        }
        if (product.getCostPrice() != null && product.getCostPrice().signum() < 0) {
            throw new IllegalArgumentException("Gharama haiwezi kuwa hasi.");
        }
    }

    private void ensureStoreExists(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new ResourceNotFoundException("Duka halijapatikana.");
        }
    }

    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
