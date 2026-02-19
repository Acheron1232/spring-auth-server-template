package com.acheron.shop.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotEmpty List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity
    ) {}
}