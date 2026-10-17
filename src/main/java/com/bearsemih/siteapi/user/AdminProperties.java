package com.bearsemih.siteapi.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credentials for bootstrapping the first admin account. Both empty means "don't create one". */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
