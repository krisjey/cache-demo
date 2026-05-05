package com.example.demo.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class ProductCacheEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheEventPublisher.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic productCacheEventTopic;

    public ProductCacheEventPublisher(RedisTemplate<String, Object> redisTemplate, ChannelTopic productCacheEventTopic) {
        this.redisTemplate = redisTemplate;
        this.productCacheEventTopic = productCacheEventTopic;
    }

    public void publish(ProductCacheEvent event) {
        redisTemplate.convertAndSend(productCacheEventTopic.getTopic(), event);
        log.info("Product cache event published. channel={}, event={}", productCacheEventTopic.getTopic(), event);
    }
}
