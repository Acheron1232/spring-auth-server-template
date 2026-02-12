package com.acheron.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "cars", indexes = {
        @Index(name = "idx_external_id", columnList = "externalId", unique = true)
})
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String externalId;

    private String url;
    private String make;
    private String model;
    private Integer price;
    private Integer mileage;
    private String firstRegistration;
    private String fuelType;
    private String power;

    private String transmission;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String title;

    public String toSemanticString() {
        return String.format(
                """
                Vehicle: %s %s %s.
                Price: %d EUR.
                Specs: %d km mileage, %s fuel, %s transmission, %s power.
                Seller Description: %s
                """,
                firstRegistration != null ? firstRegistration : "Used", make, model,
                price,
                mileage, fuelType, transmission != null ? transmission : "Unknown", power,
                description != null ? description.replace("\n", " ") : "No description"
        );
    }
}