
package com.leamary.codepilotai;

import com.leamary.codepilotai.dto.AuthResponse;
import com.leamary.codepilotai.dto.LoginRequest;
import com.leamary.codepilotai.dto.RegisterRequest;
import com.leamary.codepilotai.entity.User;
import com.leamary.codepilotai.repository.UserRepository;
import com.leamary.codepilotai.service.AuthService;
import com.leamary.codepilotai.service.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);

        passwordEncoder = new BCryptPasswordEncoder();

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void shouldRegisterNewUser() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Sample User");
        request.setEmail("sample@codepilot.test");
        request.setPassword("SamplePass@123");

        when(userRepository.existsByEmail(
                request.getEmail()
        )).thenReturn(false);

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(jwtService.generateToken(request.getEmail()))
                .thenReturn("sample-jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("Sample User", response.getName());
        assertEquals(
                "sample@codepilot.test",
                response.getEmail()
        );
        assertEquals(
                "sample-jwt-token",
                response.getToken()
        );

        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void shouldHashPasswordBeforeSavingUser() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Sample User");
        request.setEmail("sample@codepilot.test");
        request.setPassword("SamplePass@123");

        when(userRepository.existsByEmail(
                request.getEmail()
        )).thenReturn(false);

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {

                    User user = invocation.getArgument(0);

                    assertNotEquals(
                            request.getPassword(),
                            user.getPassword()
                    );

                    assertTrue(
                            passwordEncoder.matches(
                                    request.getPassword(),
                                    user.getPassword()
                            )
                    );

                    return user;
                });

        when(jwtService.generateToken(request.getEmail()))
                .thenReturn("sample-jwt-token");

        authService.register(request);

        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Sample User");
        request.setEmail("sample@codepilot.test");
        request.setPassword("SamplePass@123");

        when(userRepository.existsByEmail(
                request.getEmail()
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request)
        );

        assertEquals(
                409,
                exception.getStatusCode().value()
        );

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectShortPassword() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Sample User");
        request.setEmail("sample@codepilot.test");
        request.setPassword("123");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request)
        );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    @Test
    void shouldLoginWithCorrectCredentials() {

        String email = "sample@codepilot.test";
        String password = "SamplePass@123";

        User user = new User(
                "Sample User",
                email,
                passwordEncoder.encode(password)
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(email))
                .thenReturn("sample-jwt-token");

        LoginRequest request = new LoginRequest();

        request.setEmail(email);
        request.setPassword(password);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(email, response.getEmail());
        assertEquals(
                "sample-jwt-token",
                response.getToken()
        );
    }

    @Test
    void shouldRejectIncorrectPassword() {

        String email = "sample@codepilot.test";

        User user = new User(
                "Sample User",
                email,
                passwordEncoder.encode("SamplePass@123")
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();

        request.setEmail(email);
        request.setPassword("WrongPassword");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(
                401,
                exception.getStatusCode().value()
        );

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void shouldRejectUnknownUser() {

        String email = "unknown@codepilot.test";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();

        request.setEmail(email);
        request.setPassword("SamplePass@123");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(
                401,
                exception.getStatusCode().value()
        );

        verify(jwtService, never()).generateToken(anyString());
    }
}

