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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        when(userService.isEmailTaken("semih@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest(" Semih@Example.com ", "password123", "Semih")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userService, never()).createAccount(anyString(), anyString(), anyString(), any());
    }

    @Test
    void registerStoresNormalizedEmailAndHashedPassword() {
        when(userService.isEmailTaken("semih@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userService.createAccount("semih@example.com", "hashed", "Semih", Role.USER))
                .thenReturn(new User("semih@example.com", "hashed", "Semih", Role.USER));
        when(jwtService.generateToken("semih@example.com", Role.USER)).thenReturn("token");
        when(jwtService.getExpiration()).thenReturn(Duration.ofHours(1));

        AuthResponse response = authService.register(
                new RegisterRequest(" Semih@Example.com ", "password123", " Semih "));

        verify(userService).createAccount("semih@example.com", "hashed", "Semih", Role.USER);
        assertThat(response).isEqualTo(new AuthResponse("token", "Bearer", 3600));
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = new User("semih@example.com", "hashed", "Semih", Role.USER);
        when(userService.findAccount("semih@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("semih@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(userService.findAccount("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
    }
}
