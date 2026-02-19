package com.acheron.shop.service;

import com.acheron.shop.dto.ProductRequest;
import com.acheron.shop.dto.ProductResponse;
import com.acheron.shop.entity.Product;
import com.acheron.shop.repository.CategoryRepository;
import com.acheron.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String search, UUID categoryId, Pageable pageable) {
        return productRepository.findAllFiltered(search, categoryId, pageable)
                .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProduct(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = buildProduct(request, new Product());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public Optional<ProductResponse> updateProduct(UUID id, ProductRequest request) {
        return productRepository.findById(id)
                .map(p -> {
                    Product updatedProduct = buildProduct(request, p);
                    return ProductResponse.from(productRepository.save(updatedProduct));
                });
    }

    @Transactional
    public boolean deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            return false;
        }
        productRepository.deleteById(id);
        return true;
    }

    private Product buildProduct(ProductRequest r, Product p) {
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setStock(r.stock());
        p.setImageUrl(r.imageUrl());
        p.setSize(r.size());
        p.setColor(r.color());
        p.setBrand(r.brand());
        if (r.categoryId() != null) {
            categoryRepository.findById(r.categoryId()).ifPresent(p::setCategory);
        }
        return p;
    }
}