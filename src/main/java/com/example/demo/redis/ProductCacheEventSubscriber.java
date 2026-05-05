package com.example.demo.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class ProductCacheEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheEventSubscriber.class);

    private final CacheManager redisCacheManager;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    public ProductCacheEventSubscriber(
            @Qualifier("redisCacheManager") CacheManager redisCacheManager,
            ObjectMapper objectMapper,
            @Value("${app.instance-id:local}") String instanceId
    ) {
        this.redisCacheManager = redisCacheManager;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
    }

    public void handleMessage(String message) {
        try {
            ProductCacheEvent event = objectMapper.readValue(message, ProductCacheEvent.class);
            log.info("Product cache event received. instanceId={}, message={}", instanceId, message);
            Cache cache = redisCacheManager.getCache(RedisCacheConfig.REDIS_PRODUCTS);
            if (cache != null) {
                cache.evict(event.productId());
                log.info("Redis cache evicted by event. instanceId={}, cacheName={}, productId={}", instanceId, RedisCacheConfig.REDIS_PRODUCTS, event.productId());
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid product cache event message. instanceId={}, message={}, error={}", instanceId, message, e.getMessage());
        }
    }
}
