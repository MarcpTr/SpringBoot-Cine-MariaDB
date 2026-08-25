package com.marcptr.cine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.marcptr.cine.dto.request.UpdateMaxSessionsRequest;
import com.marcptr.cine.service.SessionPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final SessionPolicyService sessionPolicyService;

    @PutMapping("/max-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateMaxSessions(@Valid @RequestBody UpdateMaxSessionsRequest request) {
        sessionPolicyService.updateMaxSessions(request.maxSessions());
        return ResponseEntity.noContent().build();
    }
}