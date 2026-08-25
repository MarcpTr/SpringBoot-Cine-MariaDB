package com.marcptr.cine.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.marcptr.cine.dto.common.ApiResponse;
import com.marcptr.cine.dto.response.ActiveSessionResponse;
import com.marcptr.cine.model.User;
import com.marcptr.cine.service.TokenService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Validated
public class SessionController {

    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActiveSessionResponse>>> getMySessions(
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(tokenService.getActiveSessions(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        tokenService.revokeSession(user, id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping()
    public ResponseEntity<Void> revokeAllSession(Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        tokenService.revokeAllUserTokens(user);

        return ResponseEntity.noContent().build();
    }
}