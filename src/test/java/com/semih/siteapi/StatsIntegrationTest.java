package com.semih.siteapi;

import com.jayway.jsonpath.JsonPath;
import com.semih.siteapi.post.PostRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Privacy-friendly view counting and the admin statistics, against a real PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
class StatsIntegrationTest {

    private static final String FIREFOX = "Mozilla/5.0 (X11; Linux x86_64) Firefox/131.0";
    private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0) Chrome/130.0";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        postRepository.deleteAll();   // view counts go with their posts (ON DELETE CASCADE)
        userRepository.deleteAll();
        userRepository.save(new User("admin@example.com", passwordEncoder.encode("password123"), "Admin", Role.ADMIN));
        adminToken = login();
    }

    @Test
    void eachVisitorCountsOnceAndBotsAndOptOutsNotAtAll() throws Exception {
        String popular = createPost("Spring Boot ile JWT", true);
        String other = createPost("Docker notları", true);

        view(popular, "203.0.113.5", FIREFOX).andExpect(status().isNoContent());
        view(popular, "203.0.113.5", FIREFOX).andExpect(status().isNoContent()); // refresh: not counted again
        view(popular, "203.0.113.5", CHROME);                                    // other browser, other visitor
        view(popular, "198.51.100.7", FIREFOX);
        view(popular, "198.51.100.8", "Mozilla/5.0 (compatible; bingbot/2.0)");  // bot
        view(popular, "198.51.100.9", "").andExpect(status().isNoContent());     // no user agent: not a browser
        mockMvc.perform(post("/api/posts/{slug}/views", popular)
                        .header(HttpHeaders.USER_AGENT, FIREFOX)
                        .header("DNT", "1")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.10");
                            return request;
                        }))
                .andExpect(status().isNoContent());
        view(other, "203.0.113.5", FIREFOX);

        mockMvc.perform(get("/api/admin/stats/views").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViews").value(4))
                .andExpect(jsonPath("$.daily.length()").value(30))
                .andExpect(jsonPath("$.daily[29].views").value(4))
                .andExpect(jsonPath("$.topPosts[0].slug").value(popular))
                .andExpect(jsonPath("$.topPosts[0].views").value(3))
                .andExpect(jsonPath("$.topPosts[1].views").value(1));
    }

    @Test
    void draftsCannotBeCounted() throws Exception {
        String draft = createPost("Taslak", false);

        view(draft, "203.0.113.5", FIREFOX).andExpect(status().isNotFound());
        view("yok-boyle-bir-yazi", "203.0.113.5", FIREFOX).andExpect(status().isNotFound());
    }

    @Test
    void statisticsAreForAdminsOnlyAndCheckThePeriod() throws Exception {
        mockMvc.perform(get("/api/admin/stats/views")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/stats/views")
                        .param("from", "2026-10-10").param("to", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid date range"));
    }

    private ResultActions view(String slug, String ip, String userAgent) throws Exception {
        return mockMvc.perform(post("/api/posts/{slug}/views", slug)
                .header(HttpHeaders.USER_AGENT, userAgent)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                }));
    }

    /** @return the slug */
    private String createPost(String title, boolean published) throws Exception {
        String body = mockMvc.perform(post("/api/admin/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"%s\", \"content\": \"İçerik\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        if (published) {
            long id = ((Number) JsonPath.read(body, "$.id")).longValue();
            mockMvc.perform(post("/api/admin/posts/{id}/publish", id)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
        return JsonPath.read(body, "$.slug");
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
