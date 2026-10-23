package com.semih.siteapi.common.exception;

/** A tag name that cannot be turned into a URL (e.g. only symbols or non-Latin letters). */
public class InvalidTagException extends RuntimeException {

    public InvalidTagException(String name) {
        super("The tag '" + name + "' needs at least one Latin letter or digit");
    }
}
