package com.marcptr.cine.config;

import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
  CaffeineCache search =
            new CaffeineCache(
                    "search",
                    Caffeine.newBuilder()
                            .maximumSize(1000)
                            .expireAfterWrite(Duration.ofHours(6))
                            .build());

    CaffeineCache movies =
            new CaffeineCache(
                    "movies",
                    Caffeine.newBuilder()
                            .maximumSize(1000)
                            .expireAfterAccess(Duration.ofHours(6))
                            .build());

    CaffeineCache trends =
            new CaffeineCache(
                    "trends",
                    Caffeine.newBuilder()
                            .maximumSize(100)
                            .expireAfterWrite(Duration.ofMinutes(15))
                            .build());
      CaffeineCache trendingDay =
            new CaffeineCache(
                "tmdbTrendingDay",
                Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(Duration.ofMinutes(20))
                    .build()
            );

        CaffeineCache trendingWeek =
            new CaffeineCache(
                "tmdbTrendingWeek",
                Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(Duration.ofHours(4))
                    .build()
            );
    manager.setCaches(List.of(movies, trends, trendingDay, trendingWeek,search));

    return manager;
    }
}