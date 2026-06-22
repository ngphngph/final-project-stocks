package com.stockwatch.project_stock_data.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

// Railway 上的 Redis 偶爾會斷線/逾時（常見於閒置一段時間後第一次連線），
// 預設情況下 @Cacheable 讀取/寫入快取失敗會直接把例外往外丟，導致整個 API
// request 跟著失敗（這就是「有時候打得到、有時候 Failed to load stock data」的成因）。
// 這裡把快取的錯誤處理改成：失敗就記錄一下警告，當作 cache miss / 不快取，
// 直接往下查資料庫，讓使用者最多只是少了快取加速，而不是整頁壞掉。
@Slf4j
@Configuration
public class CacheErrorHandlerConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache GET failed (cache={}, key={}), falling back to DB: {}",
                        cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT failed (cache={}, key={}): {}",
                        cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache EVICT failed (cache={}, key={}): {}",
                        cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache CLEAR failed (cache={}): {}", cache.getName(), exception.toString());
            }
        };
    }
}
