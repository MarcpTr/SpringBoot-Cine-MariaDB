package com.marcptr.cine.dto.request;

import com.marcptr.cine.model.enums.Period;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrendingRequest {
    @NotNull
    private Period period = Period.DAY;
     @Min(value = 1, message = "{error.INVALID_PAGE}")
    @Max(value = 12, message = "{error.INVALID_PAGE}")
    private Integer page = 1;
}
