
package com.leamary.codepilotai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {

        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void shouldHashPassword() {

        String password = "SamplePassword@123";

        String hashedPassword =
                passwordEncoder.encode(password);

        assertNotNull(hashedPassword);
        assertNotEquals(password, hashedPassword);
    }

    @Test
    void shouldMatchCorrectPassword() {

        String password = "SamplePassword@123";

        String hashedPassword =
                passwordEncoder.encode(password);

        assertTrue(
                passwordEncoder.matches(
                        password, hashedPassword
                )
        );
    }

    @Test
    void shouldRejectIncorrectPassword() {

        String password = "SamplePassword@123";

        String hashedPassword =
                passwordEncoder.encode(password);

        assertFalse(
                passwordEncoder.matches(
                        "WrongPassword", hashedPassword
                )
        );
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {

        String password = "SamplePassword@123";

        String firstHash =
                passwordEncoder.encode(password);

        String secondHash =
                passwordEncoder.encode(password);

        assertNotEquals(firstHash, secondHash);

        assertTrue(
                passwordEncoder.matches(password, firstHash)
        );

        assertTrue(
                passwordEncoder.matches(password, secondHash)
        );
    }
}

