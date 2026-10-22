package com.semih.siteapi;

import com.jayway.jsonpath.JsonPath;
import com.semih.siteapi.contact.ContactMessageRepository;
import com.semih.siteapi.user.Role;
import com.semih.siteapi.user.User;
import com.semih.siteapi.user.UserRepository;
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

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contact form end to end. The rate limiter lives in a shared Spring context, so
 * every test sends from its own IP address to start with a clean limit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
class ContactIntegrationTest {

    private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

    private static final String VALID_MESSAGE = """
            {"name": "Ayşe", "email": "ayse@example.com", "subject": "Proje teklifi",
             "message": "Merhaba Semih, birlikte çalışalım mı?"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactMessageRepository contactMessageRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ip;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        ip = "10.0.0." + NEXT_IP.getAndIncrement();
        contactMessageRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User("admin@example.com", passwordEncoder.encode("password123"), "Admin", Role.ADMIN));
        adminToken = login("admin@example.com", "password123");
    }

    @Test
    void visitorSendsAMessageAndAdminReadsIt() throws Exception {
        send(VALID_MESSAGE).andExpect(status().isAccepted());

        String inbox = mockMvc.perform(get("/api/admin/contact-messages").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("ayse@example.com"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(inbox, "$.content[0].id")).longValue();

        mockMvc.perform(post("/api/admin/contact-messages/{id}/read", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(delete("/api/admin/contact-messages/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
        assertThat(contactMessageRepository.count()).isZero();
    }

    @Test
    void fourthMessageWithinTenMinutesGets429WithRetryAfter() throws Exception {
        send(VALID_MESSAGE).andExpect(status().isAccepted());
        send(VALID_MESSAGE).andExpect(status().isAccepted());
        send(VALID_MESSAGE).andExpect(status().isAccepted());

        send(VALID_MESSAGE)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.title").value("Too many requests"));

        assertThat(contactMessageRepository.count()).isEqualTo(3);
    }

    @Test
    void invalidMessageReturnsFieldErrors() throws Exception {
        send("""
                {"name": "", "email": "not-an-email", "message": "kısa"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    void honeypotMessageLooksAcceptedButIsNotStored() throws Exception {
        send("""
                {"name": "Bot", "email": "bot@example.com", "message": "Ucuz takipçi satın al!", "website": "http://spam"}
                """)
                .andExpect(status().isAccepted());

        assertThat(contactMessageRepository.count()).isZero();
    }

    @Test
    void inboxIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/admin/contact-messages"))
                .andExpect(status().isUnauthorized());

        userRepository.save(new User("semih@example.com", passwordEncoder.encode("password123"), "Semih", Role.USER));
        String userToken = login("semih@example.com", "password123");

        mockMvc.perform(get("/api/admin/contact-messages").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    private ResultActions send(String json) throws Exception {
        return mockMvc.perform(post("/api/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                }));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
