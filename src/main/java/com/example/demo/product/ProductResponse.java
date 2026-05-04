package com.example.demo.product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        ProductCategory category,
        int price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
