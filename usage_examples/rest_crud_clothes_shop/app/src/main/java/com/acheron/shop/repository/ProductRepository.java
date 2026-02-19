package com.acheron.shop.repository;

import com.acheron.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT p FROM Product p WHERE " +
            "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) " +
            "OR LOWER(p.brand) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> findAllFiltered(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);
}