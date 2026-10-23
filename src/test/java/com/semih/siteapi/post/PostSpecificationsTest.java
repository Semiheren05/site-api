package com.semih.siteapi.post;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostSpecificationsTest {

    @Test
    void likeWildcardsInTheSearchTextAreTakenLiterally() {
        assertThat(PostSpecifications.escapeLike("100%")).isEqualTo("100\\%");
        assertThat(PostSpecifications.escapeLike("snake_case")).isEqualTo("snake\\_case");
        assertThat(PostSpecifications.escapeLike("C:\\temp")).isEqualTo("C:\\\\temp");
        assertThat(PostSpecifications.escapeLike("Spring Boot")).isEqualTo("Spring Boot");
    }
}
