package com.example.demo.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/redis-cache")
public class RedisCacheController {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/keys")
    public List<Map<String, Object>> keys() {
        Set<String> keys = redisTemplate.keys("cache:*::*");
        if (keys == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        keys.stream()
                .sorted(Comparator.naturalOrder())
                .forEach(key -> result.add(Map.of(
                        "key", key,
                        "ttlSeconds", ttlSeconds(key)
                )));
        return result;
    }

    @GetMapping("/value/products/{productId}")
    public Map<String, Object> productCacheValue(@PathVariable("productId") Long productId) {
        String key = productKey(productId);
        Object value = redisTemplate.opsForValue().get(key);
        return Map.of(
                "key", key,
                "ttlSeconds", ttlSeconds(key),
                "value", value == null ? "<null>" : value
        );
    }

    @DeleteMapping("/keys")
    public Map<String, Object> clearRedisKeys() {
        Set<String> keys = redisTemplate.keys("cache:*::*");
        long deleted = 0;
        if (keys != null && !keys.isEmpty()) {
            deleted = redisTemplate.delete(keys);
        }
        return Map.of("deleted", deleted);
    }

    private String productKey(Long productId) {
        return "cache:" + RedisCacheConfig.REDIS_PRODUCTS + "::" + productId;
    }

    private long ttlSeconds(String key) {
        Duration ttl = redisTemplate.getExpire(key);
        if (ttl == null) {
            return -2;
        }
        return ttl.toSeconds();
    }
}
