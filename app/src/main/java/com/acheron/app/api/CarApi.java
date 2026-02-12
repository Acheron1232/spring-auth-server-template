package com.acheron.app.api;


import com.acheron.app.dto.request.CarRequest;
import com.acheron.app.dto.request.ScrapingRequest;
import com.acheron.app.entity.Car;
import com.acheron.app.service.CarService;
import com.acheron.app.service.impl.AutoScoutParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CarApi {

    private final CarService carService;
    private final AutoScoutParser autoScoutParser;

    @GetMapping
    public List<Car> findBestDeals(
//            @PageableDefault(size = 20, page = 0, sort = "name") Pageable pageable,
//            @RequestBody CarRequest carRequest
    ) {
         autoScoutParser.startScraping(new ScrapingRequest("https://www.autoscout24.com/lst/bmw/8-series-(all)"));
    return null;
    }
}
