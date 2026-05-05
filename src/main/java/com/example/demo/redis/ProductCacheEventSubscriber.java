package com.example.demo.redis;

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
    private final String instanceId;

    public ProductCacheEventSubscriber(
            @Qualifier("redisCacheManager") CacheManager redisCacheManager,
            @Value("${app.instance-id:local}") String instanceId
    ) {
        this.redisCacheManager = redisCacheManager;
        this.instanceId = instanceId;
    }

    public void handleMessage(ProductCacheEvent event) {
        log.info("Product cache event received. instanceId={}, event={}", instanceId, event);
        Cache cache = redisCacheManager.getCache(RedisCacheConfig.REDIS_PRODUCTS);
        if (cache != null) {
            cache.evict(event.productId());
            log.info("Redis cache evicted by event. instanceId={}, cacheName={}, productId={}", instanceId, RedisCacheConfig.REDIS_PRODUCTS, event.productId());
        }
    }
}
