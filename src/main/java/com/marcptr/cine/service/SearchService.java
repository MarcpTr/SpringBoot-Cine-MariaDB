package com.marcptr.cine.service;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import java.time.Instant;
import org.apache.commons.codec.digest.DigestUtils;
import com.marcptr.cine.dto.response.SearchResponse;
import com.marcptr.cine.integration.tmdb.client.TmdbClient;
import com.marcptr.cine.integration.tmdb.dto.TmdbSearchMovieResponse;
import com.marcptr.cine.model.Search;
import com.marcptr.cine.repository.SearchRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SearchService {
  private final TmdbClient tmdbClient;
  private final SearchRepository sRepository;

  @Cacheable(value = "search", key = "#query + '-' + #page + '-' + #lang")
  public SearchResponse getSearch(String query, Integer page, String lang) {
    String rawKey = page + "|" + lang + "|" + query;
    String id = DigestUtils.sha256Hex(rawKey);

    Search search = sRepository.findById(rawKey)
        .orElseGet(() -> fetchAndSaveSearch(id, query, page, lang));
    return SearchResponse.builder().content(search.getContent()).build();
  }

  private Search fetchAndSaveSearch(String id, String query, Integer page, String lang) {
    String rawKey = page + "|" + lang + "|" + query;

    TmdbSearchMovieResponse tmdb = tmdbClient.searchMovies(query, page, lang);
    Instant now = Instant.now();
    Search search = Search.builder()
        .content(tmdb.getContent())
        .id(rawKey)
        .updatedAt(now)
        .lastAccessedAt(now)
        .page(page)
        .build();

    sRepository.save(search);

    return search;
  }
}
