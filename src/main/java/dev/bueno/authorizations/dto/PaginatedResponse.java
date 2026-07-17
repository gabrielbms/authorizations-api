package dev.bueno.authorizations.dto;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        String nextToken
) {}