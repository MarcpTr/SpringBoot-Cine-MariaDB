package com.marcptr.cine.model;

import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    @Id
    private String id;

    private Long movieId;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode content;

    private String lang;

    private Instant updatedAt;

    private Instant lastAccessedAt;

    private boolean notFound;

}