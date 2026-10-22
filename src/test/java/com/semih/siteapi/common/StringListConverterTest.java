package com.semih.siteapi.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void listIsStoredCommaSeparatedAndReadBack() {
        String column = converter.convertToDatabaseColumn(List.of("Java 21", "Spring Boot"));

        assertThat(column).isEqualTo("Java 21,Spring Boot");
        assertThat(converter.convertToEntityAttribute(column)).containsExactly("Java 21", "Spring Boot");
    }

    @Test
    void emptyOrMissingListBecomesEmptyColumnAndBack() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isEmpty();
        assertThat(converter.convertToDatabaseColumn(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }
}
