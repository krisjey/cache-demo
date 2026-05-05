package com.example.demo.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class LoggingCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET failed. cacheName={}, key={}, fallback=db-query, error={}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT failed. cacheName={}, key={}, ignore=true, error={}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT failed. cacheName={}, key={}, ignore=true, error={}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR failed. cacheName={}, ignore=true, error={}", cache.getName(), exception.getMessage());
    }
}
