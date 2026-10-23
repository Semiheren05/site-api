package com.semih.siteapi.common;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** The {@code Location} header of a 201 Created response. */
public final class Locations {

    private Locations() {
    }

    /** The current request's URL followed by the new resource's id, e.g. /api/admin/posts/7. */
    public static URI of(Object id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
