package com.semih.siteapi.common.exception;

public class SlugAlreadyUsedException extends RuntimeException {

    public SlugAlreadyUsedException(String slug) {
        super("The slug '" + slug + "' is already in use");
    }
}
