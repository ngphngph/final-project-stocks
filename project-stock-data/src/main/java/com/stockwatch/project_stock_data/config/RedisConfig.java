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
        // 必須開啟 default typing，序列化時才會把 class 資訊（@class）寫進 JSON。
        // 沒開的話，從 Redis 讀回來的物件會被 Jackson 還原成普通的 LinkedHashMap，
        // 而不是原本的 DTO 類別（例如 StockDetailDTO），方法回傳時做隱含型別轉換就會丟出
        // ClassCastException: LinkedHashMap cannot be cast to StockDetailDTO。
        // 這就是「有時候正常、有時候 Failed to load stock data」的真正原因：
        // cache miss 時直接從資料庫算出正確型別的 DTO 沒問題，cache hit 時從 Redis
        // 還原出來的卻是型別錯誤的 LinkedHashMap，整個 request 就壞掉。
        // 這個 Redis 只給自己這個服務寫入跟讀取，不會收到外部不信任的資料，
        // 所以用 enableUnsafeDefaultTyping() 圖個方便；如果未來 Redis 對外開放或
        // 接收第三方資料，應該改成 enableDefaultTyping(PolymorphicTypeValidator) 限縮可還原的型別。
        GenericJacksonJsonRedisSerializer valueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();

        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .entryTtl(Duration.ofMinutes(30));  // 30分鐘失效（stock-detail 等預設快取）

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // heatmap-data 現在改成讀取 LiveQuoteService 背景持續更新的報價快取，
                // 計算成本低，TTL 縮短為 1 分鐘，讓即時報價能更快反映在前端
                .withCacheConfiguration("heatmap-data", config.entryTtl(Duration.ofMinutes(1)))
                .build();
    }
}