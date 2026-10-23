package com.semih.siteapi;

import com.semih.siteapi.auth.RefreshTokenRepository;
import com.semih.siteapi.common.RequestIdFilter;
import com.semih.siteapi.user.Role;
import com.semih.siteapi.user.User;
import com.semih.siteapi.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests against a real PostgreSQL started by Testcontainers,
 * including the Flyway migrations. Skipped automatically when Docker is not available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /** Deleting the users also deletes their refresh tokens (ON DELETE CASCADE). */
    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerThenLoginThenFetchCurrentUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "semih@example.com", "password": "password123", "displayName": "Semih"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/users/me"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        String token = login("semih@example.com", "password123");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("semih@example.com"))
                .andExpect(jsonPath("$.displayName").value("Semih"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void refreshTokensWorkOnceAndAReusedOneEndsTheLogin() throws Exception {
        saveUser("semih@example.com", "password123", Role.USER);
        String first = JsonPath.read(loginBody("semih@example.com", "password123"), "$.refreshToken");

        String refreshed = refresh(first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshExpiresIn").value(30 * 24 * 3600))
                .andReturn().getResponse().getContentAsString();
        String second = JsonPath.read(refreshed, "$.refreshToken");
        String accessToken = JsonPath.read(refreshed, "$.accessToken");
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // The first token was already used: whoever sends it again may have stolen it
        refresh(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid refresh token"));
        // ...so the whole login is revoked, the newest token too. This also proves the revocation
        // was committed even though the request ended with an exception (noRollbackFor).
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsTheLogin() throws Exception {
        saveUser("semih@example.com", "password123", Role.USER);
        String token = JsonPath.read(loginBody("semih@example.com", "password123"), "$.refreshToken");

        logout(token).andExpect(status().isNoContent());

        refresh(token).andExpect(status().isUnauthorized());
        // Logging out twice, or with a token that never existed, is not an error
        logout(token).andExpect(status().isNoContent());
        logout("never-issued").andExpect(status().isNoContent());
    }

    @Test
    void storedRefreshTokensAreHashed() throws Exception {
        saveUser("semih@example.com", "password123", Role.USER);
        String token = JsonPath.read(loginBody("semih@example.com", "password123"), "$.refreshToken");

        assertThat(refreshTokenRepository.findByTokenHash(token)).isEmpty();
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                // The request id filter runs before Spring Security, so even a 401 carries it
                .andExpect(header().exists(RequestIdFilter.HEADER));
    }

    @Test
    void springsOwnErrorsUseTheSameShape() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "short", "displayName": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }

    @Test
    void registeringTheSameEmailTwiceReturns409() throws Exception {
        String body = """
                {"email": "semih@example.com", "password": "password123", "displayName": "Semih"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already in use"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        saveUser("semih@example.com", "password123", Role.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "semih@example.com", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }

    @Test
    void regularUserCannotAccessAdminEndpoints() throws Exception {
        saveUser("semih@example.com", "password123", Role.USER);
        String token = login("semih@example.com", "password123");

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void adminCanListUsers() throws Exception {
        saveUser("admin@example.com", "password123", Role.ADMIN);
        saveUser("semih@example.com", "password123", Role.USER);
        String token = login("admin@example.com", "password123");

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    private void saveUser(String email, String password, Role role) {
        userRepository.save(new User(email, passwordEncoder.encode(password), "Test", role));
    }

    private String login(String email, String password) throws Exception {
        return JsonPath.read(loginBody(email, password), "$.accessToken");
    }

    private String loginBody(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"%s\"}".formatted(refreshToken)));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"%s\"}".formatted(refreshToken)));
    }
}
