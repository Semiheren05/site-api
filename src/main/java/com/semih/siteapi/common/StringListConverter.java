package com.semih.siteapi.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Stores a short list of strings in one comma-separated column, e.g. a project's
 * technologies ["Java", "Spring Boot"] as "Java,Spring Boot". Fine for small lists
 * that are always read together; values must not contain commas (validated in the DTO).
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(SEPARATOR, values);
    }

    @Override
    public List<String> convertToEntityAttribute(String column) {
        return column == null || column.isBlank() ? List.of() : List.of(column.split(SEPARATOR));
    }
}
