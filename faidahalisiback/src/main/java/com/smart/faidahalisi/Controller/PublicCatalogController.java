package com.smart.faidahalisi.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.faidahalisi.Dto.PublicProductResponse;
import com.smart.faidahalisi.Model.Products;
import com.smart.faidahalisi.Model.Store;
import com.smart.faidahalisi.Repository.ProductRepository;

@RestController
@RequestMapping("/api/catalog")
public class PublicCatalogController {
    private final ProductRepository products;

    public PublicCatalogController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping("/products")
    public List<PublicProductResponse> allProducts() {
        return products.findAll().stream()
                .filter(p -> p.getStatus() == null || !p.getStatus().equalsIgnoreCase("INACTIVE"))
                .map(this::toPublicProduct)
                .toList();
    }

    @GetMapping("/products/{id}")
    public PublicProductResponse product(@PathVariable Long id) {
        Products p = products.findById(id)
                .orElseThrow(() -> new com.smart.faidahalisi.Exception.ResourceNotFoundException("Bidhaa haipo."));
        return toPublicProduct(p);
    }

    @GetMapping("/products/store/{storeId}")
    public List<PublicProductResponse> productsForStore(@PathVariable Long storeId) {
        return products.findByStoreId(storeId).stream()
                .filter(p -> p.getStatus() == null || !p.getStatus().equalsIgnoreCase("INACTIVE"))
                .map(this::toPublicProduct)
                .toList();
    }

    private PublicProductResponse toPublicProduct(Products p) {
        Store s = p.getStore();
        PublicProductResponse.PublicStore store = s == null ? null :
                new PublicProductResponse.PublicStore(s.getId(), s.getStoreName(), s.getSlug(),
                        s.getDescription(), s.getLogoUrl());
        return new PublicProductResponse(p.getId(), p.getName(), p.getPrice(), p.getImageUrl(),
                p.getStatus(), p.getCategory(), null, store);
    }
}
