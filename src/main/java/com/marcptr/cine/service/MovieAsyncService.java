package com.marcptr.cine.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.cache.Cache;
import com.marcptr.cine.integration.tmdb.client.TmdbClient;
import com.marcptr.cine.integration.tmdb.dto.TmdbMovieResponse;
import com.marcptr.cine.model.Movie;
import com.marcptr.cine.repository.MovieRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MovieAsyncService {

    private final TmdbClient tmdbClient;
    private final MovieRepository mRepository;
    private final CacheManager cacheManager;

    @Async
    public void refreshMovie(
            long movieId,
            String lang,
            String cacheKey,
            String refreshKey,
            ConcurrentHashMap<String, Boolean> refreshInProgress) {

        Cache movieCache = cacheManager.getCache("movies");

        try {

            TmdbMovieResponse tmdb = tmdbClient.getMovie(movieId, lang);

            Instant now = Instant.now();
            Movie updated = Movie.builder()
                    .id(movieId + "-" + lang)
                    .lang(lang)
                    .movieId(movieId)
                    .content(tmdb.getContent())
                    .updatedAt(now)
                    .lastAccessedAt(now)
                    .build();

            mRepository.save(updated);

            if (movieCache != null) {
                movieCache.put(cacheKey, updated);
            }

        } catch (Exception e) {
            System.out.println("Error refreshing movie " + movieId + " (" + lang + ")");
            e.printStackTrace();
        } finally {
            refreshInProgress.remove(refreshKey);
        }
    }

    @Async
    public void updateLastAccess(Movie document) {

        Instant now = Instant.now();

        if (document.getLastAccessedAt() != null &&
                Duration.between(
                        document.getLastAccessedAt(),
                        now).toHours() < 24) {

            return;
        }

        document.setLastAccessedAt(now);

        mRepository.save(document);
    }
}