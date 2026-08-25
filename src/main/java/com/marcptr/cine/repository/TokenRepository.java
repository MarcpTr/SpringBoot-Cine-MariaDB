package com.marcptr.cine.repository;

import com.marcptr.cine.model.Token;
import com.marcptr.cine.model.User;
import com.marcptr.cine.model.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {
        Optional<Token> findByJti(String jti);

        List<Token> findAllByUserAndExpiredFalseAndRevokedFalse(User user);

        List<Token> findAllByUserIdAndTokenTypeAndExpiredFalseAndRevokedFalse(
                        User user,
                        TokenType tokenType);

        Optional<Token> findByToken(String token);

        List<Token> findAllByUserAndTokenTypeAndExpiredFalseAndRevokedFalseOrderByCreatedAtDesc(
                        User user,
                        TokenType tokenType);

}
