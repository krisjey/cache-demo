package com.example.demo.redis;

import com.example.demo.common.ApiResponse;
import com.example.demo.product.ProductResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api")
public class RedisProductController {

    private final RedisProductService redisProductService;

    public RedisProductController(RedisProductService redisProductService) {
        this.redisProductService = redisProductService;
    }

    @GetMapping("/redis-cache/products/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable("productId") Long productId) {
        return measured("redis-cache", () -> redisProductService.getProduct(productId));
    }

    @PatchMapping("/db-only/products/{productId}/price")
    public ApiResponse<ProductResponse> changePriceDbOnly(
            @PathVariable("productId") Long productId,
            @RequestParam("price") int price
    ) {
        return measured("db-only-update", () -> redisProductService.changePriceDbOnly(productId, price));
    }

    @PatchMapping("/evict/products/{productId}/price")
    public ApiResponse<ProductResponse> changePriceWithEvict(
            @PathVariable("productId") Long productId,
            @RequestParam("price") int price
    ) {
        return measured("explicit-eviction", () -> redisProductService.changePriceWithEvict(productId, price));
    }

    @PatchMapping("/event/products/{productId}/price")
    public ApiResponse<ProductResponse> changePriceWithEvent(
            @PathVariable("productId") Long productId,
            @RequestParam("price") int price
    ) {
        return measured("event-invalidation", () -> redisProductService.changePriceWithEvent(productId, price));
    }

    @PatchMapping("/redis-cache/products/{productId}/evict")
    public ApiResponse<String> evictProduct(@PathVariable("productId") Long productId) {
        return measured("manual-redis-evict", () -> {
            redisProductService.evictProduct(productId);
            return "evicted productId=" + productId;
        });
    }

    private <T> ApiResponse<T> measured(String scenario, Supplier<T> supplier) {
        long start = System.nanoTime();
        T data = supplier.get();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        return ApiResponse.of(scenario, elapsedMs, data);
    }
}
