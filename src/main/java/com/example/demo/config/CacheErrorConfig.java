package com.example.demo.config;

import com.example.demo.redis.LoggingCacheErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheErrorConfig implements CachingConfigurer {

    private final boolean failOpen;

    public CacheErrorConfig(@Value("${app.cache.fail-open:true}") boolean failOpen) {
        this.failOpen = failOpen;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        if (failOpen) {
            return new LoggingCacheErrorHandler();
        }
        return new SimpleCacheErrorHandler();
    }
}
