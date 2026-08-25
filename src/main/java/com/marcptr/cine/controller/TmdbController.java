package com.marcptr.cine.controller;

import org.springframework.web.bind.annotation.RestController;
import com.marcptr.cine.dto.common.ApiResponse;
import com.marcptr.cine.dto.request.SearchRequest;
import com.marcptr.cine.dto.request.TrendingRequest;
import com.marcptr.cine.dto.response.MovieResponse;
import com.marcptr.cine.dto.response.SearchResponse;
import com.marcptr.cine.integration.tmdb.dto.TmdbTrendResponse;
import com.marcptr.cine.service.MovieService;
import com.marcptr.cine.service.SearchService;
import com.marcptr.cine.service.TrendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/data")
public class TmdbController {

    private final MovieService mService;
    private final TrendService tService;
    private final SearchService sService;

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<TmdbTrendResponse>> getTrending(@Valid TrendingRequest tRequest, Locale locale) {
        switch (tRequest.getPeriod()) {
            case WEEK:
                return ResponseEntity.ok(ApiResponse.ok(tService.getTrendingWeek(tRequest.getPage(),
                        locale.getLanguage() + "-" + locale.getCountry())));
            case DAY:
                return ResponseEntity.ok(ApiResponse.ok(
                        tService.getTrendingDay(tRequest.getPage(), locale.getLanguage() + "-" + locale.getCountry())));
            default:
                return ResponseEntity.ok(ApiResponse.ok(
                        tService.getTrendingDay(tRequest.getPage(), locale.getLanguage() + "-" + locale.getCountry())));
        }
    }

    @GetMapping("/movie/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovie(@PathVariable long id, Locale locale) {
        return ResponseEntity.ok(ApiResponse.ok(mService.getMovie(id,
                locale.getLanguage() + "-" + locale.getCountry())));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> search(@Valid SearchRequest sRequest, Locale locale) {
        return ResponseEntity.ok(ApiResponse.ok(sService.getSearch(sRequest.getQuery(),
                sRequest.getPage(), locale.getLanguage() + "-" + locale.getCountry())));
    }
}
