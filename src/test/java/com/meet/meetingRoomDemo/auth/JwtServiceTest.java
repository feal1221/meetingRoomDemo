package com.meet.meetingRoomDemo.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-chars-long!!";
    private static final long   ACCESS_EXP  = 3_600_000L;  // 1 hour
    private static final long   REFRESH_EXP = 604_800_000L; // 7 days

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", ACCESS_EXP);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", REFRESH_EXP);
    }

    // ─── Access Token ─────────────────────────────────────────────────────────

    @Test
    void generateAccessToken_isValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@test.com", 0);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractUserId_matchesOriginal() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@test.com", 0);

        assertEquals(userId, jwtService.extractUserId(token));
    }

    @Test
    void extractEmail_matchesOriginal() {
        String email = "alice@example.com";
        String token = jwtService.generateAccessToken(UUID.randomUUID(), email, 1);

        assertEquals(email, jwtService.extractEmail(token));
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    @Test
    void generateRefreshToken_isValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void refreshToken_extractUserId_matchesOriginal() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId);

        assertEquals(userId, jwtService.extractUserId(token));
    }

    // ─── 無效 Token ───────────────────────────────────────────────────────────

    @Test
    void expiredToken_isInvalid() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        // 建立一個已過期的 token
        String expiredToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .expiration(new Date(System.currentTimeMillis() - 1_000)) // 過去 1 秒
            .signWith(key)
            .compact();

        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    void tamperedToken_isInvalid() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "test@test.com", 0);
        // 竄改最後幾個字元
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void randomString_isInvalid() {
        assertFalse(jwtService.isTokenValid("this.is.not.a.jwt"));
    }

    @Test
    void emptyString_isInvalid() {
        assertFalse(jwtService.isTokenValid(""));
    }

    // ─── 不同 secret 簽署的 Token ──────────────────────────────────────────────

    @Test
    void tokenSignedWithDifferentSecret_isInvalid() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
            "other-secret-key-that-is-also-32-chars-long!!".getBytes(StandardCharsets.UTF_8));
        String foreignToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(otherKey)
            .compact();

        assertFalse(jwtService.isTokenValid(foreignToken));
    }
}
