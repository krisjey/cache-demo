package com.example.demo.product;

import com.example.demo.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse getNoCacheProduct(Long productId) {
        return findProductSlowly(productId);
    }

    @Cacheable(cacheNames = CacheConfig.LOCAL_PRODUCTS, key = "#productId")
    public ProductResponse getLocalCacheProduct(Long productId) {
        return findProductSlowly(productId);
    }

    @Cacheable(cacheNames = CacheConfig.TTL_PRODUCTS, key = "#productId")
    public ProductResponse getTtlCacheProduct(Long productId) {
        return findProductSlowly(productId);
    }

    @Cacheable(cacheNames = CacheConfig.SIZE_LIMIT_PRODUCTS, key = "#productId")
    public ProductResponse getSizeLimitCacheProduct(Long productId) {
        return findProductSlowly(productId);
    }

    @Cacheable(cacheNames = CacheConfig.BAD_PRODUCT_SEARCH, key = "#category")
    public List<ProductResponse> searchWithBadKey(ProductCategory category, ProductSort sort) {
        log.info("DB search occurred. scenario=bad-key, category={}, sort={}", category, sort);
        sleep(300);
        return sortProducts(productRepository.findByCategory(category), sort);
    }

    @Cacheable(cacheNames = CacheConfig.GOOD_PRODUCT_SEARCH, key = "#category + ':' + #sort")
    public List<ProductResponse> searchWithGoodKey(ProductCategory category, ProductSort sort) {
        log.info("DB search occurred. scenario=good-key, category={}, sort={}", category, sort);
        sleep(300);
        return sortProducts(productRepository.findByCategory(category), sort);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheConfig.LOCAL_PRODUCTS,
            CacheConfig.TTL_PRODUCTS,
            CacheConfig.SIZE_LIMIT_PRODUCTS,
            CacheConfig.BAD_PRODUCT_SEARCH,
            CacheConfig.GOOD_PRODUCT_SEARCH
    }, allEntries = true)
    public ProductResponse changePrice(Long productId, int price) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found. productId=" + productId));
        product.changePrice(price);
        log.info("Product price changed. productId={}, price={}, all cache entries evicted", productId, price);
        return ProductResponse.from(product);
    }

    private ProductResponse findProductSlowly(Long productId) {
        log.info("DB query occurred. productId={}", productId);
        sleep(300);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found. productId=" + productId));
        return ProductResponse.from(product);
    }

    private List<ProductResponse> sortProducts(List<Product> products, ProductSort sort) {
        Comparator<Product> comparator = switch (sort) {
            case PRICE -> Comparator.comparingInt(Product::getPrice);
            case LATEST -> Comparator.comparing(Product::getCreatedAt).reversed();
        };
        return products.stream()
                .sorted(comparator)
                .map(ProductResponse::from)
                .toList();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during slow query simulation", e);
        }
    }
}
