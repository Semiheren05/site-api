package com.semih.siteapi.auth;

import com.semih.siteapi.auth.dto.AuthResponse;
import com.semih.siteapi.auth.dto.LoginRequest;
import com.semih.siteapi.auth.dto.RefreshRequest;
import com.semih.siteapi.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registration, login, token refresh and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and receive an access token and a refresh token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // The new account is readable at /api/users/me with the returned token
        return ResponseEntity.created(URI.create("/api/users/me")).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for an access token and a refresh token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token and a new refresh token "
            + "(each refresh token works once; 401 if invalid, expired or reused)")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "End the login the refresh token belongs to (204, also for unknown tokens)")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
