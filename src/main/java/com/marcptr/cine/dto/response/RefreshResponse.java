package com.marcptr.cine.dto.response;

public record RefreshResponse(
   String accessToken,
   String refreshToken
) {}