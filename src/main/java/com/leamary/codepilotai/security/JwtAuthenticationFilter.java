
package com.leamary.codepilotai.security;

import com.leamary.codepilotai.entity.User;
import com.leamary.codepilotai.repository.UserRepository;
import com.leamary.codepilotai.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Read the Authorization header
        String authHeader =
                request.getHeader("Authorization");

        // 2. Check whether a JWT token is present
        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the JWT token
        String token = authHeader.substring(7);

        // 4. Validate the token
        if (jwtService.isTokenValid(token) &&
                SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            // 5. Extract the user's email
            String email =
                    jwtService.extractEmail(token);

            // 6. Find the user in MySQL
            User user = userRepository
                    .findByEmail(email)
                    .orElse(null);

            if (user != null) {

                // 7. Create authentication object
                UsernamePasswordAuthenticationToken
                        authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + user.getRole()
                                        )
                                )
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // 8. Mark the user as authenticated
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        // 9. Continue processing the request
        filterChain.doFilter(request, response);
    }
}

