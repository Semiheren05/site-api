package com.semih.siteapi.contact;

import com.semih.siteapi.contact.dto.ContactRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@Tag(name = "Contact", description = "The website's contact form")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * The client IP comes from {@code getRemoteAddr()}. Behind Nginx / Cloudflare Tunnel
     * it is the real visitor's IP because {@code server.forward-headers-strategy: framework}
     * reads the X-Forwarded-For header.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a message (rate limited per IP; answers 429 with Retry-After when exceeded)")
    public void submit(@Valid @RequestBody ContactRequest request, HttpServletRequest httpRequest) {
        contactService.submit(request, httpRequest.getRemoteAddr());
    }
}
