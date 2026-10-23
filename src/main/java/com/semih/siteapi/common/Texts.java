package com.semih.siteapi.common;

/** Small text clean-ups shared by the mappers. */
public final class Texts {

    private Texts() {
    }

    /** Trims, and turns an empty or blank value into {@code null}, so "no value" is stored one way only. */
    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
