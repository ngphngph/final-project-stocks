package com.stockwatch.project_stock_data.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(GenericJacksonJsonRedisSerializer.builder().build()))
                .entryTtl(Duration.ofMinutes(30));  // 30分鐘失效（stock-detail 等預設快取）

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // heatmap-data 現在改成讀取 LiveQuoteService 背景持續更新的報價快取，
                // 計算成本低，TTL 縮短為 1 分鐘，讓即時報價能更快反映在前端
                .withCacheConfiguration("heatmap-data", config.entryTtl(Duration.ofMinutes(1)))
                .build();
    }
}