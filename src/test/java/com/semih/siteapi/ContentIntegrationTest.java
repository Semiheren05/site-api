package com.semih.siteapi;

import com.jayway.jsonpath.JsonPath;
import com.semih.siteapi.post.PostRepository;
import com.semih.siteapi.project.ProjectRepository;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Blog and portfolio flows end to end, against a real PostgreSQL with the Flyway
 * migrations. Skipped automatically when Docker is not available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
class ContentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        postRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User("admin@example.com", passwordEncoder.encode("password123"), "Admin", Role.ADMIN));
        adminToken = login("admin@example.com", "password123");
    }

    // --- Blog ---

    @Test
    void draftIsHiddenUntilPublished() throws Exception {
        String body = createPost("""
                {"title": "Spring Boot ile İlk API", "summary": "Kısa özet", "content": "# Merhaba"}
                """);
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mockMvc.perform(get("/api/posts/spring-boot-ile-ilk-api"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/posts/{id}/publish", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("spring-boot-ile-ilk-api"))
                .andExpect(jsonPath("$.content[0].content").doesNotExist());
        mockMvc.perform(get("/api/posts/spring-boot-ile-ilk-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("# Merhaba"));
    }

    @Test
    void adminSeesDraftsInTheAdminList() throws Exception {
        createPost("""
                {"title": "Taslak", "content": "..."}
                """);

        mockMvc.perform(get("/api/admin/posts").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    void sameSlugTwiceReturns409() throws Exception {
        String body = """
                {"title": "Aynı Başlık", "content": "..."}
                """;
        createPost(body);

        mockMvc.perform(post("/api/admin/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Slug already in use"));
    }

    @Test
    void invalidPostReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "slug": "Büyük Harf", "content": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.slug").exists())
                .andExpect(jsonPath("$.errors.content").exists());
    }

    @Test
    void updateAndDeletePost() throws Exception {
        long id = ((Number) JsonPath.read(createPost("""
                {"title": "Eski Başlık", "content": "..."}
                """), "$.id")).longValue();

        mockMvc.perform(put("/api/admin/posts/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Yeni Başlık", "slug": "yeni-adres", "content": "Güncel"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("yeni-adres"))
                .andExpect(jsonPath("$.title").value("Yeni Başlık"));

        mockMvc.perform(delete("/api/admin/posts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/posts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void writingContentRequiresAdmin() throws Exception {
        String body = """
                {"title": "Deneme", "content": "..."}
                """;
        mockMvc.perform(post("/api/admin/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        userRepository.save(new User("semih@example.com", passwordEncoder.encode("password123"), "Semih", Role.USER));
        String userToken = login("semih@example.com", "password123");

        mockMvc.perform(post("/api/admin/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // --- Portfolio ---

    @Test
    void publicPortfolioShowsOnlyPublishedProjectsFeaturedFirst() throws Exception {
        createProject("""
                {"title": "Gizli Proje", "summary": "Henüz hazır değil", "published": false}
                """);
        createProject("""
                {"title": "Java Denemeleri", "summary": "Günlük alıştırmalar", "displayOrder": 1, "published": true}
                """);
        createProject("""
                {"title": "Site API", "summary": "Web sitemin backend'i", "featured": true, "displayOrder": 5,
                 "technologies": ["Java 21", "Spring Boot", "PostgreSQL"],
                 "repoUrl": "https://github.com/Semiheren05/site-api", "published": true}
                """);

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("site-api"))
                .andExpect(jsonPath("$[0].technologies.length()").value(3))
                .andExpect(jsonPath("$[0].technologies[1]").value("Spring Boot"))
                .andExpect(jsonPath("$[1].slug").value("java-denemeleri"));

        mockMvc.perform(get("/api/projects/gizli-proje")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/site-api")).andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/projects").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void invalidProjectReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/admin/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Proje", "summary": "", "repoUrl": "not a url", "displayOrder": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.summary").exists())
                .andExpect(jsonPath("$.errors.repoUrl").exists())
                .andExpect(jsonPath("$.errors.displayOrder").exists());
    }

    private String createPost(String json) throws Exception {
        return create("/api/admin/posts", json);
    }

    private String createProject(String json) throws Exception {
        return create("/api/admin/projects", json);
    }

    private String create(String path, String json) throws Exception {
        return mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
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
