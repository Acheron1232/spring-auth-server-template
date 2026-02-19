package com.acheron.shop.dto;

import com.acheron.shop.entity.Order;
import com.acheron.shop.entity.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String userId,
        String status,
        BigDecimal totalAmount,
        List<ItemResponse> items,
        Instant createdAt
) {
    public record ItemResponse(UUID productId, String productName, Integer quantity, BigDecimal unitPrice) {}

    public static OrderResponse from(Order o) {
        List<ItemResponse> items = o.getItems().stream()
                .map(i -> new ItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice()))
                .toList();
        return new OrderResponse(o.getId(), o.getUserId(), o.getStatus().name(),
                o.getTotalAmount(), items, o.getCreatedAt());
    }
}