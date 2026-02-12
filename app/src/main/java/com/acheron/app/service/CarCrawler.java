package com.acheron.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarCrawler {
    private final Set<CarParser>  carParsers;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

//    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.HOURS)
    public void saveCars() {
        log.info("Starting scheduled crawling job...");
        for (CarParser parser : carParsers) {
            executor.submit(() -> {
                parser.fetchCars();
            });
        }
//        List<Future<?>> futures = new ArrayList<>();
//        Set<Car> cars = ConcurrentHashMap.newKeySet();
//        for (CarParser carParser : carParsers) {
//            Future<?> future = executor.submit(() -> {
//                cars.addAll(carParser.fetchCars());
//            });
//            futures.add(future);
//        }

//        //wait
//        for (Future<?> future : futures) {
//            try {
//                future.get();
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("Error waiting for task completion", e);
//            }
//        }
//
//        if (!cars.isEmpty()) {
//            carService.saveAll(cars);
//        }

    }
}
