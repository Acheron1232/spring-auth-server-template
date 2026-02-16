package com.acheron.app.api;

import com.acheron.app.entity.Product;
import com.acheron.app.repo.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApi {

    private final ProductRepository productRepository;

    public record ProductCreateRequest(
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 200) String name,
            @NotNull @Min(0) Long priceCents
    ) {
    }

    public record ProductResponse(UUID id, String sku, String name, long priceCents) {
        public static ProductResponse fromEntity(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getPriceCents());
        }
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productRepository.findAll().stream().map(ProductResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductCreateRequest req) {
        if (productRepository.findBySku(req.sku()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Product p = new Product(UUID.randomUUID(), req.sku(), req.name(), req.priceCents(), Instant.now());
        Product saved = productRepository.save(p);
        return ResponseEntity.ok(ProductResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @RequestBody @Valid ProductCreateRequest req) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setSku(req.sku());
                    existing.setName(req.name());
                    existing.setPriceCents(req.priceCents());
                    Product saved = productRepository.save(existing);
                    return ResponseEntity.ok(ProductResponse.fromEntity(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
