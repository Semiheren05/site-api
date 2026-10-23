package com.semih.siteapi.auth;

import com.semih.siteapi.auth.dto.AuthResponse;
import com.semih.siteapi.auth.dto.LoginRequest;
import com.semih.siteapi.auth.dto.RegisterRequest;
import com.semih.siteapi.common.exception.EmailAlreadyUsedException;
import com.semih.siteapi.common.exception.InvalidCredentialsException;
import com.semih.siteapi.security.JwtService;
import com.semih.siteapi.user.Role;
import com.semih.siteapi.user.User;
import com.semih.siteapi.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userService.isEmailTaken(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        User user = userService.createAccount(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                Role.USER);
        log.info("User registered userId={}", user.getId());
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userService.findAccount(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return AuthResponse.bearer(token, jwtService.getExpiration().toSeconds());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
