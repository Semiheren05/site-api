package com.semih.siteapi.common.exception;

/** Thrown when no slug was given and none could be built from the title (e.g. a title of only symbols). */
public class InvalidSlugException extends RuntimeException {

    public InvalidSlugException() {
        super("Could not build a slug from the title; please provide one");
    }
}
