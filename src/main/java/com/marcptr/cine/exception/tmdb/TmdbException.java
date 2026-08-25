package com.marcptr.cine.exception.tmdb;

import com.marcptr.cine.model.enums.ErrorCode;

public class TmdbException  extends RuntimeException {

    private final ErrorCode code;
    private final Object details;

    public TmdbException(ErrorCode code) {
        this(code, null);
    }

    public TmdbException(ErrorCode code, Object details) {
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