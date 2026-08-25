package com.marcptr.cine.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import com.marcptr.cine.dto.response.ActiveSessionResponse;
import com.marcptr.cine.exception.ResourceNotFoundException;
import com.marcptr.cine.model.Token;
import com.marcptr.cine.model.User;
import com.marcptr.cine.model.enums.ErrorCode;
import com.marcptr.cine.model.enums.TokenType;
import com.marcptr.cine.repository.TokenRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final JwtService jwtService;

    public void saveUserToken(User user, String jwtToken, TokenType tokenType) {
        if (tokenType != TokenType.REFRESH) {
            return;
        }
        String jti = jwtService.extractJti(jwtToken);

        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(tokenType)
                .jti(jti)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    public Token findByJti(String jti) {
        return tokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Invalid session"));
    }

    public void revokeAllUserTokens(User user) {
        List<Token> validTokens = tokenRepository.findAllByUserAndExpiredFalseAndRevokedFalse(user);
        if (validTokens.isEmpty())
            return;
        validTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    public void revokeToken(String tokenValue) {
        tokenRepository.findByToken(tokenValue).ifPresent(token -> {
            token.setExpired(true);
            token.setRevoked(true);
            tokenRepository.save(token);
        });
    }

    public Token findByToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("JWT revocado o inexistente"));
    }

    public List<ActiveSessionResponse> getActiveSessions(User user) {

        return tokenRepository
                .findAllByUserAndTokenTypeAndExpiredFalseAndRevokedFalseOrderByCreatedAtDesc(
                        user,
                        TokenType.REFRESH)
                .stream()
                .map(token -> new ActiveSessionResponse(
                        token.getId(),
                        token.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public void revokeSession(User user, UUID tokenId) {

        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                        Map.of("error", "ID session not found")));

        if (!token.getUser().equals(user)) {
            throw new AccessDeniedException("Not your session");
        }

        token.setExpired(true);
        token.setRevoked(true);

        tokenRepository.save(token);
    }

    @Transactional
    public void revokeAllExcept(User user, String currentRefreshToken) {

        List<Token> tokens = tokenRepository
                .findAllByUserIdAndTokenTypeAndExpiredFalseAndRevokedFalse(
                        user,
                        TokenType.REFRESH);

        for (Token token : tokens) {
            if (!token.getToken().equals(currentRefreshToken)) {
                token.setExpired(true);
                token.setRevoked(true);
            }
        }

        tokenRepository.saveAll(tokens);
    }

    @Transactional
    public void enforceSessionLimit(User user, int maxSessions) {

        List<Token> activeRefreshTokens = tokenRepository
                .findAllByUserIdAndTokenTypeAndExpiredFalseAndRevokedFalse(
                        user,
                        TokenType.REFRESH);

        if (activeRefreshTokens.size() < maxSessions) {
            return;
        }

        activeRefreshTokens.sort(
                Comparator.comparing(
                        Token::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        int tokensToRevoke = activeRefreshTokens.size() - maxSessions + 1;

        for (int i = 0; i < tokensToRevoke; i++) {
            Token token = activeRefreshTokens.get(i);
            token.setExpired(true);
            token.setRevoked(true);
        }

        tokenRepository.saveAll(activeRefreshTokens);
    }
}
