package com.acheron.shop.controller;

import com.acheron.shop.dto.OrderRequest;
import com.acheron.shop.dto.OrderResponse;
import com.acheron.shop.entity.Order;
import com.acheron.shop.entity.OrderItem;
import com.acheron.shop.repository.OrderRepository;
import com.acheron.shop.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public Page<OrderResponse> myOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = jwt.getSubject();
        return orderRepository.findByUserId(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(OrderResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderRepository.findById(id)
                .filter(o -> o.getUserId().equals(jwt.getSubject()))
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.items()) {
            var product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.productId()));

            if (product.getStock() < itemReq.quantity()) {
                return ResponseEntity.badRequest().build();
            }

            product.setStock(product.getStock() - itemReq.quantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(product.getPrice())
                    .build();
            items.add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }

        Order order = Order.builder()
                .userId(jwt.getSubject())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(total)
                .build();

        Order saved = orderRepository.save(order);
        items.forEach(i -> i.setOrder(saved));
        saved.setItems(items);
        orderRepository.save(saved);

        return ResponseEntity.ok(OrderResponse.from(saved));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderRepository.findById(id)
                .filter(o -> o.getUserId().equals(jwt.getSubject()))
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .map(o -> {
                    o.setStatus(Order.OrderStatus.CANCELLED);
                    return ResponseEntity.ok(OrderResponse.from(orderRepository.save(o)));
                })
                .orElse(ResponseEntity.badRequest().build());
    }
}