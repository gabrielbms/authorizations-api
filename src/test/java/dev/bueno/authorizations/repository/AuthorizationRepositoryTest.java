package dev.bueno.authorizations.repository;

import dev.bueno.authorizations.domain.Authorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<Authorization> table;

    @Mock
    private DynamoDbIndex<Authorization> index;

    @Mock
    private PageIterable<Authorization> pageIterable;

    private AuthorizationRepository repository;

    @BeforeEach
    void setUp() {
        // Mocka o comportamento de criar a tabela
        // O tipo genérico é informado explicitamente porque o Mockito não consegue inferir
        // o TableSchema<T> usando any(TableSchema.class), gerando um warning de unchecked assignment.
        when(enhancedClient.table(
                eq("Authorizations"),
                ArgumentMatchers.<TableSchema<Authorization>>any()
        )).thenReturn(table);
        repository = new AuthorizationRepository(enhancedClient);
    }

    @Test
    @DisplayName("Deve realizar query no GSI user-date-index quando userId é fornecido")
    void should_PerformQueryOnUserIndex_When_UserIdIsProvided() {
        // Arrange
        String userId = "user-123";
        when(table.index("user-date-index")).thenReturn(index);
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(pageIterable);
        when(pageIterable.iterator()).thenReturn(Collections.emptyIterator());

        // Act
        repository.search(userId, null, null, null, null);

        // Assert: Verifica se o índice correto foi buscado
        verify(table).index("user-date-index");

        // Assert: Captura a requisição para validar o que foi enviado ao SDK
        ArgumentCaptor<QueryEnhancedRequest> captor = ArgumentCaptor.forClass(QueryEnhancedRequest.class);
        verify(index).query(captor.capture());

        assertNotNull(captor.getValue().queryConditional());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o userId não é informado")
    void should_ThrowException_When_SearchParamsAreMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                repository.search(null, null, null, null, null)
        );
    }
}