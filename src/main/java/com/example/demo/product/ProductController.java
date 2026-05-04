package com.example.demo.product;

import com.example.demo.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/no-cache/products/{productId}")
    public ApiResponse<ProductResponse> getNoCacheProduct(@PathVariable Long productId) {
        return measured("no-cache", () -> productService.getNoCacheProduct(productId));
    }

    @GetMapping("/local-cache/products/{productId}")
    public ApiResponse<ProductResponse> getLocalCacheProduct(@PathVariable Long productId) {
        return measured("local-cache", () -> productService.getLocalCacheProduct(productId));
    }

    @GetMapping("/ttl-cache/products/{productId}")
    public ApiResponse<ProductResponse> getTtlCacheProduct(@PathVariable Long productId) {
        return measured("ttl-cache", () -> productService.getTtlCacheProduct(productId));
    }

    @GetMapping("/size-limit-cache/products/{productId}")
    public ApiResponse<ProductResponse> getSizeLimitCacheProduct(@PathVariable Long productId) {
        return measured("size-limit-cache", () -> productService.getSizeLimitCacheProduct(productId));
    }

    @GetMapping("/bad-key/products/search")
    public ApiResponse<List<ProductResponse>> searchWithBadKey(
            @RequestParam ProductCategory category,
            @RequestParam ProductSort sort
    ) {
        return measured("bad-key", () -> productService.searchWithBadKey(category, sort));
    }

    @GetMapping("/good-key/products/search")
    public ApiResponse<List<ProductResponse>> searchWithGoodKey(
            @RequestParam ProductCategory category,
            @RequestParam ProductSort sort
    ) {
        return measured("good-key", () -> productService.searchWithGoodKey(category, sort));
    }

    @PatchMapping("/products/{productId}/price")
    public ApiResponse<ProductResponse> changePrice(@PathVariable Long productId, @RequestParam int price) {
        return measured("change-price-with-cache-evict", () -> productService.changePrice(productId, price));
    }

    private <T> ApiResponse<T> measured(String scenario, Supplier<T> supplier) {
        long start = System.nanoTime();
        T data = supplier.get();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        return ApiResponse.of(scenario, elapsedMs, data);
    }
}
