package dev.bueno.authorizations.repository;

import dev.bueno.authorizations.domain.Authorization;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Repository
public class AuthorizationRepository {

    private final DynamoDbTable<Authorization> table;

    public AuthorizationRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Authorizations", TableSchema.fromBean(Authorization.class));
    }

    @PostConstruct
    public void createTableIfNotExists() {
        try {
            table.createTable(builder -> builder
                    .globalSecondaryIndices(
                            gsi -> gsi.indexName("user-date-index")
                                    .projection(p -> p.projectionType(ProjectionType.ALL))
                                    .provisionedThroughput(pt -> pt.readCapacityUnits(5L).writeCapacityUnits(5L))
                    )
            );
        } catch (ResourceInUseException e) {
            // A tabela já existe. Exceção ignorada propositalmente para não poluir o log.
        }
    }

    public Authorization save(Authorization authorization) {
        authorization.setAuthorizationId(UUID.randomUUID().toString());
        authorization.setCreatedAt(Instant.now().toString()); // ISO-8601 padrão UTC
        table.putItem(authorization);
        return authorization;
    }

    public Page<Authorization> search(String userId, String status, String startDate, String endDate, Map<String, AttributeValue> startKey) {
        if (userId != null) {
            // Estratégia: Filtra grande volume na camada de disco usando a Sort Key (data) do índice,
            // e aplica o filtro de status na memória (FilterExpression) para economizar custos de infraestrutura.
            return executeQuery(userId, startDate, endDate, status, startKey);
        }
        throw new IllegalArgumentException("Busca inválida: É obrigatório fornecer userId.");
    }

    private Page<Authorization> executeQuery(String partitionValue, String startDate, String endDate,
                                             String filterValue, Map<String, AttributeValue> startKey) {

        DynamoDbIndex<Authorization> index = table.index("user-date-index");
        QueryConditional queryConditional;

        // Estratégia de montagem dinâmica da query usando a Sort Key createdAt
        if (startDate != null && endDate != null) {
            queryConditional = QueryConditional.sortBetween(
                    Key.builder().partitionValue(partitionValue).sortValue(startDate).build(),
                    Key.builder().partitionValue(partitionValue).sortValue(endDate).build()
            );
        } else if (startDate != null) {
            queryConditional = QueryConditional.sortGreaterThanOrEqualTo(Key.builder().partitionValue(partitionValue).sortValue(startDate).build());
        } else if (endDate != null) {
            queryConditional = QueryConditional.sortLessThanOrEqualTo(Key.builder().partitionValue(partitionValue).sortValue(endDate).build());
        } else {
            queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(partitionValue).build());
        }

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .limit(20); // Tamanho fixo de página definido para o escopo da avaliação

        if (startKey != null) {
            requestBuilder.exclusiveStartKey(startKey);
        }

        // Aplica filtros que não são chaves de partição/ordenação
        if (filterValue != null && !filterValue.trim().isEmpty()) {
            Expression filterExpression = Expression.builder()
                    .expression("#statusAttr = :val")
                    .putExpressionName("#statusAttr", "status")
                    .putExpressionValue(":val", AttributeValue.builder().s(filterValue).build())
                    .build();
            requestBuilder.filterExpression(filterExpression);
        }

        var iterator = index.query(requestBuilder.build()).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }
}