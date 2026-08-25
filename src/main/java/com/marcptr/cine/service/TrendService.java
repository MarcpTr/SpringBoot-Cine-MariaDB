package com.marcptr.cine.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.marcptr.cine.integration.tmdb.client.TmdbClient;
import com.marcptr.cine.integration.tmdb.dto.TmdbTrendResponse;
import com.marcptr.cine.model.enums.Period;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TrendService {
    private final TmdbClient tmdbClient;

    @Cacheable(value = "tmdbTrendingDay", key = "#page + '-' + #lang")
    public TmdbTrendResponse getTrendingDay(int page, String lang) {
        return tmdbClient.getTrend(Period.DAY, page, lang);
    }

    @Cacheable(value = "tmdbTrendingWeek", key = "#page + '-' + #lang")
    public TmdbTrendResponse getTrendingWeek(int page, String lang) {
        return tmdbClient.getTrend(Period.WEEK, page, lang);
    }
}
