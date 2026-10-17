package com.semih.siteapi.auth;

import com.semih.siteapi.auth.dto.AuthResponse;
import com.semih.siteapi.auth.dto.LoginRequest;
import com.semih.siteapi.auth.dto.RegisterRequest;
import com.semih.siteapi.common.exception.EmailAlreadyUsedException;
import com.semih.siteapi.common.exception.InvalidCredentialsException;
import com.semih.siteapi.security.JwtService;
import com.semih.siteapi.user.Role;
import com.semih.siteapi.user.User;
import com.semih.siteapi.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        User user = userRepository.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                Role.USER));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
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
