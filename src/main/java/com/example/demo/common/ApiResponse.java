package com.example.demo.common;

public record ApiResponse<T>(
        String scenario,
        long elapsedMs,
        T data
) {
    public static <T> ApiResponse<T> of(String scenario, long elapsedMs, T data) {
        return new ApiResponse<>(scenario, elapsedMs, data);
    }
}
