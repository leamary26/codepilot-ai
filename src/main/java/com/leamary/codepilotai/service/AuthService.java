
package com.leamary.codepilotai.service;

import com.leamary.codepilotai.dto.AuthResponse;
import com.leamary.codepilotai.dto.LoginRequest;
import com.leamary.codepilotai.dto.RegisterRequest;
import com.leamary.codepilotai.entity.User;
import com.leamary.codepilotai.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // 1. Register a new user
    public AuthResponse register(
            RegisterRequest request) {

        // Validate name
        if (request.getName() == null ||
                request.getName().isBlank() ||
                request.getName().trim().length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter a valid name."
            );
        }

        // Validate email
        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter your email."
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (email.length() > 254 ||
                !email.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter a valid email address."
            );
        }

        // Validate password
        if (request.getPassword() == null ||
                request.getPassword().length() < 8 ||
                request.getPassword().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain at least 8 characters."
            );
        }

        // BCrypt supports passwords up to 72 bytes
        if (request.getPassword()
                .getBytes(StandardCharsets.UTF_8)
                .length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password is too long."
            );
        }

        // Check whether email already exists
        if (userRepository.existsByEmail(email)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered."
            );
        }

        // Hash password using BCrypt
        String encodedPassword =
                passwordEncoder.encode(
                        request.getPassword()
                );

        // Create new user
        User user = new User(
                request.getName().trim(),
                email,
                encodedPassword
        );

        // Save user in MySQL
        try {

            userRepository.saveAndFlush(user);

        } catch (DataIntegrityViolationException e) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unable to register. Email may already be registered."
            );
        }

        // Generate JWT token
        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        // Return registration response
        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                "Registration successful!"
        );
    }

    // 2. Login an existing user
    public AuthResponse login(
            LoginRequest request) {

        // Validate login details
        if (request.getEmail() == null ||
                request.getEmail().isBlank() ||
                request.getPassword() == null ||
                request.getPassword().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please enter your email and password."
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Find user in MySQL
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password."
                        )
                );

        // Check password length
        if (request.getPassword()
                .getBytes(StandardCharsets.UTF_8)
                .length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password."
            );
        }

        // Verify password using BCrypt
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password."
            );
        }

        // Generate JWT token
        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        // Return login response
        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail(),
                "Login successful!"
        );
    }
}

