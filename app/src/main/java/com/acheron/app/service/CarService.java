package com.acheron.app.service;

import com.acheron.app.dto.request.CarRequest;
import com.acheron.app.entity.Car;
import com.acheron.app.entity.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final VectorStore vectorStore;

    @Transactional
    public void saveAll(Set<Car> cars) {
        List<Document> documentsToAdd = new ArrayList<>();

        for (Car car : cars) {
            if (carRepository.findByExternalId(car.getExternalId()).isPresent()) {
                log.debug("Car {} already exists. Skipping.", car.getExternalId());
                continue; 
            }

            Car savedCar = carRepository.save(car);

            Map<String, Object> metadata = Map.of(
                "car_id", savedCar.getId().toString(),
                "price", savedCar.getPrice() != null ? savedCar.getPrice() : 0,
                "make", savedCar.getMake(),
                "mileage", savedCar.getMileage() != null ? savedCar.getMileage() : 0
            );

            Document doc = new Document(savedCar.toSemanticString(), metadata);
            documentsToAdd.add(doc);
        }

        if (!documentsToAdd.isEmpty()) {
            vectorStore.add(documentsToAdd);
            log.info("Saved {} new cars to DB and VectorStore", documentsToAdd.size());
        }
    }

    public List<Car> search(CarRequest userQuery) {

//        FilterExpressionBuilder b = new FilterExpressionBuilder();
//        var filterExpression = b.and(
//                b.lte("price", maxPrice),
//                b.lte("mileage", maxMileage)
//        ).build();

        List<Document> foundDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuery.prompt())
                        .topK(10)
//                        .filterExpression(filterExpression)
                        .build()
        );

        List<UUID> carIds = foundDocs.stream()
                .map(doc -> UUID.fromString((String) doc.getMetadata().get("car_id")))
                .toList();

        return carRepository.findAllById(carIds);
    }
}