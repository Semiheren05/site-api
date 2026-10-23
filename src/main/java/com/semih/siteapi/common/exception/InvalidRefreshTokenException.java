package com.semih.siteapi.common.exception;

/** Unknown, expired, revoked or reused refresh token. Deliberately one vague message for all. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("The refresh token is invalid or expired; please log in again");
    }
}
