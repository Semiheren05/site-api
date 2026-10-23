package com.semih.siteapi;

import com.jayway.jsonpath.JsonPath;
import com.semih.siteapi.post.PostRepository;
import com.semih.siteapi.tag.TagRepository;
import com.semih.siteapi.user.Role;
import com.semih.siteapi.user.User;
import com.semih.siteapi.user.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tags, tag filter and search on the public blog, against a real PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
class TagsAndSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        postRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User("admin@example.com", passwordEncoder.encode("password123"), "Admin", Role.ADMIN));
        adminToken = login();
    }

    @Test
    void postsCanBeFilteredByTagAndTheTagListCountsOnlyPublishedPosts() throws Exception {
        publish(createPost("Spring Boot ve JPA", "İçerik", "\"Spring Boot\", \"JPA\""));
        publish(createPost("N+1 problemi", "İçerik", "\"jpa\""));
        createPost("Taslak", "İçerik", "\"JPA\", \"Docker\"");

        mockMvc.perform(get("/api/posts").param("tag", "jpa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/api/posts").param("tag", "spring-boot"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tags[0].slug").value("jpa"))
                .andExpect(jsonPath("$.content[0].tags[1].name").value("Spring Boot"));

        // "jpa" and "JPA" are the same tag; Docker is only on a draft, so it is not listed
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("jpa"))
                .andExpect(jsonPath("$[0].name").value("JPA"))
                .andExpect(jsonPath("$[0].postCount").value(2))
                .andExpect(jsonPath("$[1].slug").value("spring-boot"))
                .andExpect(jsonPath("$[1].postCount").value(1));
    }

    @Test
    void searchLooksInTitleSummaryAndContentIgnoringCase() throws Exception {
        publish(createPost("Kimlik doğrulama", "Spring Security ile JWT kullandım", null));
        publish(createPost("Veritabanı", "Flyway migration'ları", null));
        createPost("Taslak JWT yazısı", "Görünmemeli", null);

        mockMvc.perform(get("/api/posts").param("q", "jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Kimlik doğrulama"));
        mockMvc.perform(get("/api/posts").param("q", "VERİTABANI").param("tag", "yok"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void wildcardsInTheSearchAreLiteral() throws Exception {
        publish(createPost("Kapsam", "Testlerde 100% kapsam hedefi", null));
        publish(createPost("Sayılar", "1000 test yazdım", null));

        mockMvc.perform(get("/api/posts").param("q", "100%"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Kapsam"));
    }

    @Test
    void tooShortSearchIsRejected() throws Exception {
        mockMvc.perform(get("/api/posts").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void aPageOfPostsCostsTheSameNumberOfQueriesHoweverManyPostsItHas() throws Exception {
        for (int i = 1; i <= 6; i++) {
            publish(createPost("Yazı " + i, "İçerik", "\"Etiket " + i + "\", \"Ortak\""));
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(6))
                .andExpect(jsonPath("$.content[0].tags.length()").value(2));

        // One query for the posts and one batch for all their tags (the page is not full, so no
        // count query). Without @BatchSize it would be 1 + 6: one more query per post.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void updatingAPostReplacesItsTags() throws Exception {
        long id = createPost("Docker notları", "İçerik", "\"Docker\", \"Linux\"");

        mockMvc.perform(put("/api/admin/posts/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Docker notları", "content": "İçerik", "tags": ["Docker Compose"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.tags[0].slug").value("docker-compose"));
    }

    @Test
    void tagsNeedALatinLetterOrDigit() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Başlık", "content": "İçerik", "tags": ["日本語"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid tag"));
    }

    /** @param tagsJson the inside of the JSON array, e.g. "\"Java\", \"JPA\"", or null for no tags */
    private long createPost(String title, String content, String tagsJson) throws Exception {
        String tags = tagsJson == null ? "" : ", \"tags\": [" + tagsJson + "]";
        String body = mockMvc.perform(post("/api/admin/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"%s\", \"content\": \"%s\"%s}".formatted(title, content, tags)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void publish(long id) throws Exception {
        mockMvc.perform(post("/api/admin/posts/{id}/publish", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
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
