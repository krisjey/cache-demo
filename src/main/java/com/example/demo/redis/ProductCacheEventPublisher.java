package com.example.demo.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class ProductCacheEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheEventPublisher.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic productCacheEventTopic;
    private final ObjectMapper objectMapper;

    public ProductCacheEventPublisher(
            StringRedisTemplate stringRedisTemplate,
            ChannelTopic productCacheEventTopic,
            ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.productCacheEventTopic = productCacheEventTopic;
        this.objectMapper = objectMapper;
    }

    public void publish(ProductCacheEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(productCacheEventTopic.getTopic(), message);
            log.info("Product cache event published. channel={}, message={}", productCacheEventTopic.getTopic(), message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize product cache event", e);
        }
    }
}
