package com.example.demo.redis;

public record ProductCacheEvent(
        Long productId,
        String eventType,
        String sourceInstance
) {
    public static ProductCacheEvent productChanged(Long productId, String sourceInstance) {
        return new ProductCacheEvent(productId, "PRODUCT_CHANGED", sourceInstance);
    }
}
