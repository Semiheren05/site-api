package com.semih.siteapi.project;

import com.semih.siteapi.project.dto.ProjectRequest;
import com.semih.siteapi.project.dto.ProjectResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMapperTest {

    private final ProjectMapper mapper = new ProjectMapper();

    @Test
    void cleansUpTheRequestOnTheWayIn() {
        ProjectRequest request = new ProjectRequest(
                "  site-api ", null, " Kişisel sitemin backend'i ", "   ",
                List.of(" Java 21", "Spring Boot ", "Java 21"),
                " https://github.com/Semiheren05/site-api ", "", true, 2, true);

        ProjectResponse response = mapper.toResponse(mapper.toEntity(request, "site-api"));

        assertThat(response.slug()).isEqualTo("site-api");
        assertThat(response.title()).isEqualTo("site-api");
        assertThat(response.summary()).isEqualTo("Kişisel sitemin backend'i");
        assertThat(response.description()).isNull();
        assertThat(response.technologies()).containsExactly("Java 21", "Spring Boot");
        assertThat(response.repoUrl()).isEqualTo("https://github.com/Semiheren05/site-api");
        assertThat(response.liveUrl()).isNull();
        assertThat(response.featured()).isTrue();
        assertThat(response.displayOrder()).isEqualTo(2);
    }

    @Test
    void missingTechnologiesBecomeAnEmptyList() {
        ProjectRequest request = new ProjectRequest("Proje", null, "Özet", null, null, null, null, false, 0, false);

        assertThat(mapper.toResponse(mapper.toEntity(request, "proje")).technologies()).isEmpty();
    }
}
