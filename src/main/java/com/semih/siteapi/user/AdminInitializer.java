package com.semih.siteapi.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first ADMIN account from ADMIN_EMAIL / ADMIN_PASSWORD on startup.
 * Idempotent: does nothing if that email already exists.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminProperties adminProperties;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(AdminProperties adminProperties,
                            UserService userService,
                            PasswordEncoder passwordEncoder) {
        this.adminProperties = adminProperties;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminProperties.isConfigured()) {
            return;
        }
        String email = adminProperties.email().trim().toLowerCase(Locale.ROOT);
        if (userService.isEmailTaken(email)) {
            return;
        }
        User admin = userService.createAccount(
                email, passwordEncoder.encode(adminProperties.password()), "Admin", Role.ADMIN);
        // Only the id: email addresses stay out of the logs
        log.info("Created admin account userId={}", admin.getId());
    }
}
