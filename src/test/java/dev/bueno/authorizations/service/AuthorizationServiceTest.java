package dev.bueno.authorizations.service;

import dev.bueno.authorizations.domain.Authorization;
import dev.bueno.authorizations.dto.PaginatedResponse;
import dev.bueno.authorizations.repository.AuthorizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final String USER_ID = "user-123";
    private static final String STATUS = "ATIVO";
    private static final String AUTHORIZATION_ID = "auth-123";
    private static final String CREATED_AT = "2026-07-14T10:00:00Z";

    @Mock
    private AuthorizationRepository repository;

    @InjectMocks
    private AuthorizationService service;

    private Authorization dummyAuthorization;

    @BeforeEach
    void setUp() {
        dummyAuthorization = new Authorization();
        dummyAuthorization.setAuthorizationId(AUTHORIZATION_ID);
        dummyAuthorization.setUserId(USER_ID);
        dummyAuthorization.setStatus(STATUS);
        dummyAuthorization.setCreatedAt(CREATED_AT);
    }

    @Test
    @DisplayName("Deve buscar e retornar itens sem erro")
    void shouldSearchAndReturnItems() {
        // Arrange
        // Mockito cria o mock usando o tipo bruto (raw type). O cast para o tipo genérico
        // é seguro neste contexto devido ao type erasure do Java.
        @SuppressWarnings("unchecked")
        Page<Authorization> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(dummyAuthorization));
        when(mockPage.lastEvaluatedKey()).thenReturn(null); // Simulando a última página

        when(repository.search(eq(USER_ID), eq(STATUS), any(), any(), any())).thenReturn(mockPage);

        // Act
        PaginatedResponse<Authorization> result = service.search(USER_ID, STATUS, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals(AUTHORIZATION_ID, result.items().getFirst().getAuthorizationId());
        assertNull(result.nextToken(), "Como é a última página, o token deve ser nulo");
    }

    @Test
    @DisplayName("Deve retornar uma resposta paginada vazia quando o repositório retornar null")
    void shouldReturnEmptyPaginatedResponseWhenRepositoryReturnsNull() {
        // Arrange
        when(repository.search(eq(USER_ID), eq(STATUS), any(), any(), any())).thenReturn(null);

        // Act
        PaginatedResponse<Authorization> result = service.search(USER_ID, STATUS, null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        assertNull(result.nextToken());
    }

    @Test
    @DisplayName("Deve retornar um nextToken quando o repositório retornar um lastEvaluatedKey")
    void shouldReturnNextTokenWhenPaginationExists() {
        // Arrange
        // Cria uma chave de paginação fake que o DynamoDB retornaria
        Map<String, AttributeValue> lastKey = Map.of(
                "userId", AttributeValue.builder().s(USER_ID).build()
        );

        // Mockito cria o mock usando o tipo bruto (raw type). O cast para o tipo genérico
        // é seguro neste contexto devido ao type erasure do Java.
        @SuppressWarnings("unchecked")
        Page<Authorization> mockPage = mock(Page.class);
        when(mockPage.items()).thenReturn(List.of(dummyAuthorization));
        when(mockPage.lastEvaluatedKey()).thenReturn(lastKey); // Simulando que há mais páginas

        when(repository.search(eq(USER_ID), eq(STATUS), any(), any(), any())).thenReturn(mockPage);

        // Act
        PaginatedResponse<Authorization> result = service.search(USER_ID, STATUS, null, null, null);

        // Assert
        assertNotNull(result);
        assertNotNull(result.nextToken(), "O token de paginação deve ser gerado quando houver lastEvaluatedKey");

        // Verifica se o token não está vazio
        assertFalse(result.nextToken().isBlank());
    }
}