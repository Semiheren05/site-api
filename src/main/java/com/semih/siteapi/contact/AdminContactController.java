package com.semih.siteapi.contact;

import com.semih.siteapi.contact.dto.ContactMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The admin inbox. Access is enforced in SecurityConfig (/api/admin/** requires ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/contact-messages")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminContactController {

    private final ContactService contactService;

    public AdminContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "Contact messages, newest first (paginated)")
    public Page<ContactMessageResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return contactService.list(pageable);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a message as read")
    public ContactMessageResponse markAsRead(@PathVariable Long id) {
        return contactService.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a message")
    public void delete(@PathVariable Long id) {
        contactService.delete(id);
    }
}
