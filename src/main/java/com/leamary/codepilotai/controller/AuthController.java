package com.leamary.codepilotai.controller;

import com.leamary.codepilotai.dto.AuthResponse;
import com.leamary.codepilotai.dto.LoginRequest;
import com.leamary.codepilotai.dto.RegisterRequest;
import com.leamary.codepilotai.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {

        this.authService = authService;
    }

    // 1. Register a new user
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    // 2. Login an existing user
    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}
