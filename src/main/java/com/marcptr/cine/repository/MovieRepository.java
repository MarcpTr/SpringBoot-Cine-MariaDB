package com.marcptr.cine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.marcptr.cine.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, String> {
}