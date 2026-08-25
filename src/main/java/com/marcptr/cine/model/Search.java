package com.marcptr.cine.model;

import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Id;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "searches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Search {

  @Id
  private String id;
  private Long query;
  private Integer page;
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode content;

  private String lang;

  private Instant updatedAt;

  private Instant lastAccessedAt;

}
