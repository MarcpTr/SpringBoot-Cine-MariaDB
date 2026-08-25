package com.marcptr.cine.service;

import java.util.UUID;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.marcptr.cine.model.User;
import com.marcptr.cine.model.enums.TokenType;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    @Value("${jwt.issuer}")
    private String issuer;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) throws JwtException {

        final Claims claims = extractAllClaims(token);

        return resolver.apply(claims);
    }

    public String extractRefreshJti(String token) {
        return extractClaim(token, claims -> claims.get("refresh_jti", String.class));
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, refreshExpiration, TokenType.REFRESH, null);
    }

    public String generateAccessToken(UserDetails userDetails, String refreshToken) {

        String refreshJti = extractJti(refreshToken);

        return generateToken(userDetails, jwtExpiration, TokenType.ACCESS, refreshJti);
    }

    private String generateToken(UserDetails userDetails,
            long expirationTime,
            TokenType type,
            String refreshJti) {

        JwtBuilder builder = Jwts.builder()
                .claim("role", ((User) userDetails).getRole().name())
                .claim("type", type.name())
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("version", "v1")
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setSubject(userDetails.getUsername())
                .setAudience("api-client")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime));

        if (type == TokenType.ACCESS && refreshJti != null) {
            builder.claim("refresh_jti", refreshJti);
        }

        return builder
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails, TokenType expectedType) {
        try {
            Claims claims = extractAllClaims(token);

            String username = claims.getSubject();

            String typeStr = claims.get("type", String.class);
            TokenType type = TokenType.valueOf(typeStr);

            return username.equals(userDetails.getUsername())
                    && type.equals(expectedType)
                    && !isTokenExpired(claims);

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
