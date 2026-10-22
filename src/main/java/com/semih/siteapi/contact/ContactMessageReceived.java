package com.semih.siteapi.contact;

/**
 * Published after a contact message is saved. The email notification listens to this
 * instead of being called directly, so a slow or broken mail server never slows down
 * or fails the visitor's request.
 */
public record ContactMessageReceived(Long id, String name, String email, String subject, String message) {

    static ContactMessageReceived from(ContactMessage saved) {
        return new ContactMessageReceived(
                saved.getId(), saved.getName(), saved.getEmail(), saved.getSubject(), saved.getMessage());
    }
}
