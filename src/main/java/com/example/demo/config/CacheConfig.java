package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig implements CachingConfigurer {

    public static final String LOCAL_PRODUCTS = "localProducts";
    public static final String TTL_PRODUCTS = "ttlProducts";
    public static final String SIZE_LIMIT_PRODUCTS = "sizeLimitProducts";
    public static final String BAD_PRODUCT_SEARCH = "badProductSearch";
    public static final String GOOD_PRODUCT_SEARCH = "goodProductSearch";

    @Primary
    @Bean("localCacheManager")
    public CacheManager localCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                caffeineCache(LOCAL_PRODUCTS, Duration.ofMinutes(30), 1_000),
                caffeineCache(TTL_PRODUCTS, Duration.ofSeconds(10), 1_000),
                caffeineCache(SIZE_LIMIT_PRODUCTS, Duration.ofMinutes(30), 3),
                caffeineCache(BAD_PRODUCT_SEARCH, Duration.ofMinutes(30), 1_000),
                caffeineCache(GOOD_PRODUCT_SEARCH, Duration.ofMinutes(30), 1_000)
        ));
        return cacheManager;
    }

    @Override
    public CacheManager cacheManager() {
        return localCacheManager();
    }

    private CaffeineCache caffeineCache(String name, Duration ttl, long maximumSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(ttl)
                .maximumSize(maximumSize)
                .build());
    }
}
