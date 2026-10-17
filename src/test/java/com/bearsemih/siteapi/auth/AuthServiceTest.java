package com.bearsemih.siteapi.auth;

import com.bearsemih.siteapi.auth.dto.AuthResponse;
import com.bearsemih.siteapi.auth.dto.LoginRequest;
import com.bearsemih.siteapi.auth.dto.RegisterRequest;
import com.bearsemih.siteapi.common.exception.EmailAlreadyUsedException;
import com.bearsemih.siteapi.common.exception.InvalidCredentialsException;
import com.bearsemih.siteapi.security.JwtService;
import com.bearsemih.siteapi.user.Role;
import com.bearsemih.siteapi.user.User;
import com.bearsemih.siteapi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        when(userRepository.existsByEmail("semih@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest(" Semih@Example.com ", "password123", "Semih")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerStoresNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmail("semih@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("semih@example.com", Role.USER)).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(Duration.ofHours(1));

        AuthResponse response = authService.register(
                new RegisterRequest(" Semih@Example.com ", "password123", " Semih "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("semih@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getDisplayName()).isEqualTo("Semih");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);

        assertThat(response).isEqualTo(new AuthResponse("token", "Bearer", 3600));
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = new User("semih@example.com", "hashed", "Semih", Role.USER);
        when(userRepository.findByEmail("semih@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("semih@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
    }
}
