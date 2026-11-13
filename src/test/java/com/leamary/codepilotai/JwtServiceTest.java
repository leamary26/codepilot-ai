
package com.leamary.codepilotai;

import com.leamary.codepilotai.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        // Generate a temporary secret for testing
        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);

        String testSecret = Base64.getEncoder()
                .encodeToString(secretBytes);

        ReflectionTestUtils.setField(
                jwtService, "secretKey", testSecret
        );

        ReflectionTestUtils.setField(
                jwtService, "expiration", 86400000L
        );
    }

    @Test
    void shouldGenerateValidToken() {

        String token = jwtService.generateToken(
                "test@codepilot.test"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void shouldExtractCorrectEmail() {

        String email = "test@codepilot.test";

        String token = jwtService.generateToken(email);

        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void shouldRejectInvalidToken() {

        assertFalse(
                jwtService.isTokenValid("invalid-token")
        );
    }

    @Test
    void shouldRejectExpiredToken() {

        ReflectionTestUtils.setField(
                jwtService, "expiration", -60000L
        );

        String token = jwtService.generateToken(
                "test@codepilot.test"
        );

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {

        String token = jwtService.generateToken(
                "test@codepilot.test"
        );

        byte[] differentSecret = new byte[32];
        new SecureRandom().nextBytes(differentSecret);

        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                Base64.getEncoder()
                        .encodeToString(differentSecret)
        );

        assertFalse(jwtService.isTokenValid(token));
    }
}
