package com.example.demo.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    public static final String REDIS_PRODUCTS = "redisProducts";
    public static final String PRODUCT_CACHE_EVENT_CHANNEL = "product-cache-events";

    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .computePrefixWith(cacheName -> "cache:" + cacheName + "::");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(REDIS_PRODUCTS, defaultConfig))
                .transactionAware()
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ChannelTopic productCacheEventTopic() {
        return new ChannelTopic(PRODUCT_CACHE_EVENT_CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter productCacheEventListenerAdapter,
            ChannelTopic productCacheEventTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(productCacheEventListenerAdapter, productCacheEventTopic);
        return container;
    }

    @Bean
    public MessageListenerAdapter productCacheEventListenerAdapter(ProductCacheEventSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handleMessage");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    private ObjectMapper redisObjectMapper() {
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.demo")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .build();
        return new ObjectMapper()
                .findAndRegisterModules()
                .activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }
}
