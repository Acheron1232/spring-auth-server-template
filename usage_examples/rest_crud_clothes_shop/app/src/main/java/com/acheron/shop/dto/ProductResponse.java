package com.acheron.shop.dto;

import com.acheron.shop.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        String size,
        String color,
        String brand,
        String categoryName,
        Instant createdAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStock(), p.getImageUrl(), p.getSize(), p.getColor(), p.getBrand(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getCreatedAt()
        );
    }
}