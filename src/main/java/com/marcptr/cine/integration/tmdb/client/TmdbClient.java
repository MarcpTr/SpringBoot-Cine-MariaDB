package com.marcptr.cine.integration.tmdb.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.marcptr.cine.exception.tmdb.TmdbException;
import com.marcptr.cine.exception.tmdb.TmdbNotFoundException;
import com.marcptr.cine.integration.tmdb.dto.TmdbMovieResponse;
import com.marcptr.cine.integration.tmdb.dto.TmdbSearchMovieResponse;
import com.marcptr.cine.integration.tmdb.dto.TmdbTrendResponse;
import com.marcptr.cine.model.enums.ErrorCode;
import com.marcptr.cine.model.enums.Period;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class TmdbClient {
        private final WebClient webClient;
        @Value("${tmdb.api-key}")
        String apiKey;

        public TmdbSearchMovieResponse searchMovies(String query, Integer page, String lang) {
                JsonNode content = webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/search/movie")
                                                .queryParam("query", query)
                                                .queryParam("lang", lang)
                                                .queryParam("page", page)
                                                .queryParam("api_key", apiKey)
                                                .build())
                                .retrieve()
                                .onStatus(
                                                HttpStatusCode::is4xxClientError,
                                                response -> Mono.error(
                                                                new TmdbException(ErrorCode.TMDB_CLIENT)))
                                .onStatus(
                                                HttpStatusCode::is5xxServerError,
                                                response -> Mono.error(
                                                                new TmdbException(ErrorCode.TMDB_SERVER)))
                                .bodyToMono(JsonNode.class)
                                .block();
                System.out.print(content);
                return TmdbSearchMovieResponse.builder().content(content).build();
        }

        public TmdbMovieResponse getMovie(long id, String lang) {
                try {
                        JsonNode content = webClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/movie/" + id)
                                                        .queryParam("language", lang)
                                                        .queryParam("append_to_response",
                                                                        "videos,credits")
                                                        .queryParam("api_key", apiKey)
                                                        .build())
                                        .retrieve()
                                        .onStatus(status -> status.value() == 404, response -> response
                                                        .bodyToMono(String.class)
                                                        .defaultIfEmpty("TMDB client error")
                                                        .flatMap(body -> Mono.error(
                                                                        new TmdbNotFoundException(
                                                                                        ErrorCode.TMDB_NOT_FOUND))))
                                        .onStatus(HttpStatusCode::is4xxClientError, response -> response
                                                        .bodyToMono(String.class)
                                                        .defaultIfEmpty("TMDB client error")
                                                        .flatMap(body -> Mono.error(
                                                                        new TmdbException(
                                                                                        ErrorCode.TMDB_CLIENT))))
                                        .onStatus(HttpStatusCode::is5xxServerError, response -> response
                                                        .bodyToMono(String.class)
                                                        .defaultIfEmpty("TMDB server error")
                                                        .flatMap(body -> Mono.error(
                                                                        new TmdbException(
                                                                                        ErrorCode.TMDB_SERVER))))

                                        .bodyToMono(JsonNode.class)

                                        .timeout(Duration.ofSeconds(5))

                                        .doOnError(error -> log.error("Error calling TMDB API for id={}", id, error))

                                        .block();
                        return TmdbMovieResponse.builder().content(content).build();

                } catch (WebClientRequestException e) {
                        throw new TmdbException(ErrorCode.TMDB_CONNECTION);
                } catch (TmdbNotFoundException e) {

                        throw e;

                } catch (TmdbException e) {
                        throw e;
                } catch (Exception e) {
                        throw new TmdbException(ErrorCode.TMDB_UNKNOWN);
                }
        }

        public TmdbTrendResponse getTrend(Period period, int page, String lang) {
                try {
                        JsonNode content = webClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("trending/movie/" + period.name().toLowerCase())
                                                        .queryParam("language", lang)
                                                        .queryParam("page", page)
                                                        .queryParam("api_key", apiKey)
                                                        .build())
                                        .retrieve()
                                        .onStatus(status -> status.value() == 404, response -> response
                                                        .bodyToMono(String.class)
                                                        .defaultIfEmpty("TMDB client error")
                                                        .flatMap(body -> Mono.error(
                                                                        new TmdbNotFoundException(
                                                                                        ErrorCode.TMDB_NOT_FOUND))))
                                        .onStatus(HttpStatusCode::is4xxClientError, response -> response
                                                        .bodyToMono(String.class)
                                                        .defaultIfEmpty("TMDB client error")
                                                        .flatMap(body -> Mono.error(
                                                                        new TmdbException(ErrorCode.TMDB_CLIENT))))
                                        .onStatus(HttpStatusCode::is5xxServerError, response -> response
                                                        .bodyToMono(String.class)
                                                        .defaultIfEmpty("TMDB server error")
                                                        .flatMap(body -> Mono.error(
                                                                        new TmdbException(ErrorCode.TMDB_SERVER))))
                                        .bodyToMono(JsonNode.class)
                                        .timeout(Duration.ofSeconds(5))
                                        .doOnError(error -> log.error(
                                                        "Error calling TMDB API for ={}",
                                                        period + " " + page + " " + lang,
                                                        error))
                                        .block();

                        return TmdbTrendResponse.builder()
                                        .content(content)
                                        .build();

                } catch (WebClientRequestException e) {
                        throw new TmdbException(ErrorCode.TMDB_CONNECTION);
                } catch (TmdbNotFoundException e) {
                        throw e;
                } catch (TmdbException e) {
                        throw e;
                } catch (Exception e) {
                        throw new TmdbException(ErrorCode.TMDB_UNKNOWN);
                }
        }
}
