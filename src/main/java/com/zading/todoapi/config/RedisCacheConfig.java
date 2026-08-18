package com.zading.todoapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@Profile("redis")
public class RedisCacheConfig {
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration TODO_DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration TODO_LOGS_TTL = Duration.ofMinutes(5);

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfiguration = createConfiguration(objectMapper, DEFAULT_TTL);

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                CacheNames.TODO_DETAIL, createConfiguration(objectMapper, TODO_DETAIL_TTL),
                CacheNames.TODO_LOGS, createConfiguration(objectMapper, TODO_LOGS_TTL)
        );

        return builder -> builder
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations);
    }

    private RedisCacheConfiguration createConfiguration(ObjectMapper objectMapper, Duration ttl) {
        GenericJackson2JsonRedisSerializer valueSerializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }
}
