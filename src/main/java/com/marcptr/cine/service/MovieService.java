
package com.marcptr.cine.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import com.marcptr.cine.dto.response.MovieResponse;
import com.marcptr.cine.exception.tmdb.TmdbNotFoundException;
import com.marcptr.cine.integration.tmdb.client.TmdbClient;
import com.marcptr.cine.integration.tmdb.dto.TmdbMovieResponse;
import com.marcptr.cine.model.Movie;
import com.marcptr.cine.model.enums.ErrorCode;
import com.marcptr.cine.repository.MovieRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MovieService {
    private final TmdbClient tmdbClient;
    private final MovieAsyncService movieAsyncService;
    private final MovieRepository mRepository;
    private final CacheManager cManager;
    private final ConcurrentHashMap<String, Boolean> refreshInProgress = new ConcurrentHashMap<>();
    private Cache movieCache;

    @PostConstruct
    public void init() {
        this.movieCache = cManager.getCache("movies");
    }

    public MovieResponse getMovie(long id, String lang) {
        String key = buildKey(id, lang);

        Movie cached = movieCache.get(key, Movie.class);

        if (cached != null) {
            return MovieResponse.builder()
                    .content(handleCachedDocument(cached, key).getContent())
                    .build();
        }

        Movie document = mRepository
                .findById(buildKey(id, lang))
                .orElse(null);

        if (document == null) {
            return MovieResponse.builder()
                    .content(fetchAndSaveMovie(id, lang).getContent()).build();
        }

        movieAsyncService.updateLastAccess(document);
        movieCache.put(key, document);

        return MovieResponse.builder()
                .content(handleCachedDocument(document, key).getContent()).build();
    }

    private Movie handleCachedDocument(Movie document, String cacheKey) {

        if (document.isNotFound()) {
            long ageDays = Duration.between(
                    document.getUpdatedAt(),
                    Instant.now()).toDays();

            if (ageDays < 1) {
                throw new TmdbNotFoundException(ErrorCode.TMDB_NOT_FOUND);
            }

            Movie refreshed = refreshSynchronously(
                    document.getMovieId(),
                    document.getLang(),
                    cacheKey);

            if (refreshed.isNotFound()) {
                throw new TmdbNotFoundException(ErrorCode.TMDB_NOT_FOUND);
            }

            return refreshed;
        }

        long ageDays = Duration.between(
                document.getUpdatedAt(),
                Instant.now()).toDays();

        if (ageDays < 7) {
            return document;
        }

        if (ageDays <= 10) {

            refreshAsyncIfNeeded(
                    document.getMovieId(),
                    document.getLang(),
                    cacheKey);

            return document;
        }

        Movie refreshed = refreshSynchronously(
                document.getMovieId(),
                document.getLang(),
                cacheKey);

        return refreshed;
    }

    private Movie refreshSynchronously(
            long movieId,
            String lang,
            String cacheKey) {

        try {

            TmdbMovieResponse tmdb = tmdbClient.getMovie(movieId, lang);

            Instant now = Instant.now();
            Movie updated = Movie.builder()
                    .id(buildKey(movieId, lang))
                    .lang(lang)
                    .movieId(movieId)
                    .content(tmdb.getContent())
                    .updatedAt(now)
                    .lastAccessedAt(now)
                    .build();

            mRepository.save(updated);

            movieCache.put(cacheKey, updated);

            return updated;
        } catch (TmdbNotFoundException e) {

            Movie notFoundDoc = Movie.builder()
                    .id(buildKey(movieId, lang))
                    .movieId(movieId)
                    .lang(lang)
                    .movieId(movieId)
                    .notFound(true)
                    .updatedAt(Instant.now())
                    .lastAccessedAt(Instant.now())
                    .build();

            mRepository.save(notFoundDoc);

            throw e;
        }

    }

    private void refreshAsyncIfNeeded(Long movieId, String lang, String cacheKey) {
        String refreshKey = buildKey(movieId, lang);

        if (refreshInProgress.putIfAbsent(
                refreshKey,
                true) != null) {
            return;
        }

        movieAsyncService.refreshMovie(
                movieId,
                lang,
                cacheKey,
                refreshKey,
                refreshInProgress);
    }

    private Movie fetchAndSaveMovie(long id, String lang) {

        try {

            TmdbMovieResponse tmdb = tmdbClient.getMovie(id, lang);

            Instant now = Instant.now();
            Movie movie = Movie.builder()
                    .id(buildKey(id, lang))
                    .lang(lang)
                    .movieId(id)
                    .content(tmdb.getContent())
                    .updatedAt(now)
                    .lastAccessedAt(now)
                    .build();

            mRepository.save(movie);

            movieCache.put(buildKey(id, lang), movie);

            return movie;

        } catch (TmdbNotFoundException e) {

            Movie notFoundDoc = Movie.builder()
                    .id(buildKey(id, lang))
                    .movieId(id)
                    .lang(lang)
                    .notFound(true)
                    .updatedAt(Instant.now())
                    .lastAccessedAt(Instant.now())
                    .build();

            mRepository.save(notFoundDoc);

            throw e;
        }
    }

    private String buildKey(long id, String lang) {
        return id + "-" + lang;
    }
}