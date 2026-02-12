package com.acheron.app.dto.request;

import com.acheron.app.dto.util.CarSearchParams;

import java.util.Set;

public record CarRequest(String prompt,
                         Set<CarSearchParams> searchParams
                         ) {
}
