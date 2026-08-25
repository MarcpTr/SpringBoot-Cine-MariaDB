package com.marcptr.cine.exception.tmdb;

import com.marcptr.cine.model.enums.ErrorCode;

public class TmdbNotFoundException  extends RuntimeException {

    private final ErrorCode code;
    private final Object details;

    public TmdbNotFoundException(ErrorCode code) {
        this(code, null);
    }

    public TmdbNotFoundException(ErrorCode code, Object details) {
        this.code = code;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }
}