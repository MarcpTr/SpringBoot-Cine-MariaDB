package com.marcptr.cine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.marcptr.cine.model.Search;

public interface SearchRepository extends JpaRepository<Search, String> {
}