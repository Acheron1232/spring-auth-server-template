package com.acheron.authserver.dto.util;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ApiError(
    int status,
    String error,
    String message,
    String path,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    Instant timestamp
) {
}