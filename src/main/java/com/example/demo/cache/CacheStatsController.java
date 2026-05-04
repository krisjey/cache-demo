package com.example.demo.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheStatsController {

    private final CacheManager cacheManager;

    public CacheStatsController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/stats")
    public List<Map<String, Object>> stats() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        return cacheNames.stream()
                .map(this::toStats)
                .toList();
    }

    @PostMapping("/cleanup")
    public Map<String, Object> cleanup() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            CaffeineCache cache = caffeineCache(cacheName);
            if (cache != null) {
                nativeCache(cache).cleanUp();
            }
        });
        return Map.of("message", "cache cleanup completed");
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clear() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        return Map.of("message", "all cache entries cleared");
    }

    private Map<String, Object> toStats(String cacheName) {
        CaffeineCache cache = caffeineCache(cacheName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cacheName", cacheName);
        if (cache == null) {
            result.put("type", "unknown");
            return result;
        }

        Cache<Object, Object> nativeCache = nativeCache(cache);
        CacheStats stats = nativeCache.stats();
        result.put("estimatedSize", nativeCache.estimatedSize());
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("hitRate", stats.hitRate());
        result.put("evictionCount", stats.evictionCount());
        result.put("averageLoadPenaltyMs", stats.averageLoadPenalty() / 1_000_000.0);
        return result;
    }

    private CaffeineCache caffeineCache(String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache caffeineCache) {
            return caffeineCache;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Cache<Object, Object> nativeCache(CaffeineCache cache) {
        return (Cache<Object, Object>) cache.getNativeCache();
    }
}
