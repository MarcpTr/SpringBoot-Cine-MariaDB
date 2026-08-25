package com.marcptr.cine.dto.response;

import java.util.UUID;

public record ActiveSessionResponse(
        UUID  tokenId,
        String createdAt
) {}