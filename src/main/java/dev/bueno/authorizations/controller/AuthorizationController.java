package dev.bueno.authorizations.controller;

import dev.bueno.authorizations.domain.Authorization;
import dev.bueno.authorizations.dto.CreateAuthorizationRequest;
import dev.bueno.authorizations.dto.PaginatedResponse;
import dev.bueno.authorizations.service.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authorizations")
public class AuthorizationController {

    private final AuthorizationService service;

    public AuthorizationController(AuthorizationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Authorization create(@Valid @RequestBody CreateAuthorizationRequest request) {
        return service.create(request.userId(), request.status());
    }

    @GetMapping
    public PaginatedResponse<Authorization> search(
            @RequestParam @NotBlank String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String nextToken) {

        return service.search(userId, status, startDate, endDate, nextToken);
    }
}