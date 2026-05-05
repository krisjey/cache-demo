package com.example.demo.redis;

import com.example.demo.product.Product;
import com.example.demo.product.ProductRepository;
import com.example.demo.product.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RedisProductService {

    private static final Logger log = LoggerFactory.getLogger(RedisProductService.class);

    private final ProductRepository productRepository;
    private final ProductCacheEventPublisher eventPublisher;
    private final CacheManager redisCacheManager;
    private final String instanceId;

    public RedisProductService(
            ProductRepository productRepository,
            ProductCacheEventPublisher eventPublisher,
            @Qualifier("redisCacheManager") CacheManager redisCacheManager,
            @Value("${app.instance-id:local}") String instanceId
    ) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.redisCacheManager = redisCacheManager;
        this.instanceId = instanceId;
    }

    @Cacheable(cacheManager = "redisCacheManager", cacheNames = RedisCacheConfig.REDIS_PRODUCTS, key = "#p0")
    public ProductResponse getProduct(Long productId) {
        log.info("DB query occurred. scenario=redis-cache, instanceId={}, productId={}", instanceId, productId);
        simulateSlowQuery();
        return ProductResponse.from(findProduct(productId));
    }

    @Transactional
    public ProductResponse changePriceDbOnly(Long productId, int price) {
        Product product = findProduct(productId);
        product.changePrice(price);
        log.info("Product price changed without cache eviction. instanceId={}, productId={}, price={}", instanceId, productId, price);
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheManager = "redisCacheManager", cacheNames = RedisCacheConfig.REDIS_PRODUCTS, key = "#p0")
    public ProductResponse changePriceWithEvict(Long productId, int price) {
        Product product = findProduct(productId);
        product.changePrice(price);
        log.info("Product price changed with explicit eviction. instanceId={}, productId={}, price={}", instanceId, productId, price);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changePriceWithEvent(Long productId, int price) {
        Product product = findProduct(productId);
        product.changePrice(price);
        ProductResponse response = ProductResponse.from(product);
        eventPublisher.publish(ProductCacheEvent.productChanged(productId, instanceId));
        log.info("Product price changed with pubsub event. instanceId={}, productId={}, price={}", instanceId, productId, price);
        return response;
    }

    public void evictProduct(Long productId) {
        Cache cache = redisCacheManager.getCache(RedisCacheConfig.REDIS_PRODUCTS);
        if (cache != null) {
            cache.evict(productId);
        }
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found. productId=" + productId));
    }

    private void simulateSlowQuery() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during slow query simulation", e);
        }
    }
}
