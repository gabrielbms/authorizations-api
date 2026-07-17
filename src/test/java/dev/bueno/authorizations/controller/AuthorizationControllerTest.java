package dev.bueno.authorizations.controller;

import dev.bueno.authorizations.domain.Authorization;
import dev.bueno.authorizations.dto.CreateAuthorizationRequest;
import dev.bueno.authorizations.dto.PaginatedResponse;
import dev.bueno.authorizations.service.AuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class AuthorizationControllerTest {

    private static final String USER_ID = "user-123";
    private static final String STATUS = "ACTIVE";
    private static final String AUTHORIZATION_ID = "auth-123";
    private static final String CREATED_AT = "2026-07-14T10:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthorizationService service;

    @Test
    @DisplayName("POST /authorizations - Deve retornar 201 Created com payload válido")
    void shouldReturn201WhenCreatingWithValidPayload() throws Exception {
        // Arrange
        CreateAuthorizationRequest request = new CreateAuthorizationRequest(USER_ID, STATUS);

        Authorization mockAuth = new Authorization();
        mockAuth.setAuthorizationId(AUTHORIZATION_ID);
        mockAuth.setUserId(USER_ID);
        mockAuth.setStatus(STATUS);
        mockAuth.setCreatedAt(CREATED_AT);

        when(service.create(USER_ID, STATUS)).thenReturn(mockAuth);

        // Act & Assert
        mockMvc.perform(post("/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorizationId").value(AUTHORIZATION_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    @DisplayName("POST /authorizations - Deve retornar 400 Bad Request se payload for inválido (faltando campos)")
    void shouldReturn400WhenPayloadIsInvalid() throws Exception {
        // Arrange: Payload com campos em branco que devem falhar no @NotBlank
        CreateAuthorizationRequest request = new CreateAuthorizationRequest("", "");

        // Act & Assert
        mockMvc.perform(post("/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("GET /authorizations - Deve retornar 200 OK e dados paginados")
    void shouldReturn200AndPaginatedData() throws Exception {
        // Arrange
        Authorization auth = new Authorization();
        auth.setAuthorizationId(AUTHORIZATION_ID);
        auth.setUserId(USER_ID);

        PaginatedResponse<Authorization> response = new PaginatedResponse<>(List.of(auth), "encoded-token");

        when(service.search(USER_ID, STATUS, null, null, null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/authorizations")
                        .param("userId", USER_ID)
                        .param("status", STATUS)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].authorizationId").value(AUTHORIZATION_ID))
                .andExpect(jsonPath("$.nextToken").value("encoded-token"));
    }

    @Test
    @DisplayName("GET /authorizations - Deve retornar 400 Bad Request quando userId estiver ausente")
    void shouldReturn400WhenUserIdIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/authorizations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("O parâmetro 'userId' é obrigatório."));
    }
}