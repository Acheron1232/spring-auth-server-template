package com.acheron.app.api;

import com.acheron.app.entity.AppUser;
import com.acheron.app.entity.Product;
import com.acheron.app.entity.PurchaseOrder;
import com.acheron.app.repo.AppUserRepository;
import com.acheron.app.repo.ProductRepository;
import com.acheron.app.repo.PurchaseOrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApi {

    private final PurchaseOrderRepository orderRepository;
    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;

    public record OrderCreateRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity
    ) {
    }

    public record OrderResponse(UUID id, UUID userId, UUID productId, int quantity, String status) {
        public static OrderResponse fromEntity(PurchaseOrder o) {
            return new OrderResponse(
                    o.getId(),
                    o.getUser().getId(),
                    o.getProduct().getId(),
                    o.getQuantity(),
                    o.getStatus().name()
            );
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("sub");
        }

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }

        List<OrderResponse> orders = orderRepository.findAllByUser_Id(user.getId()).stream()
                .map(OrderResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid OrderCreateRequest req) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("sub");
        }

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        Product product = productRepository.findById(req.productId()).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().build();
        }

        PurchaseOrder order = new PurchaseOrder(
                UUID.randomUUID(),
                user,
                product,
                req.quantity(),
                PurchaseOrder.Status.CREATED,
                Instant.now()
        );

        PurchaseOrder saved = orderRepository.save(order);
        return ResponseEntity.ok(OrderResponse.fromEntity(saved));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> setStatus(@PathVariable UUID id, @RequestParam("value") PurchaseOrder.Status status) {
        return orderRepository.findById(id)
                .map(existing -> {
                    existing.setStatus(status);
                    PurchaseOrder saved = orderRepository.save(existing);
                    return ResponseEntity.ok(OrderResponse.fromEntity(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
