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
                                    .provisionedThroughput(pt -> pt.readCapacityUnits(5L).writeCapacityUnits(5L)),
                            gsi -> gsi.indexName("status-date-index")
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
            // Busca pelo GSI 1: Pode filtrar por status usando FilterExpression
            return executeQuery("user-date-index", userId, startDate, endDate, status, "status", startKey);
        } else if (status != null) {
            // Busca pelo GSI 2: Sem filtro adicional
            return executeQuery("status-date-index", status, startDate, endDate, null, null, startKey);
        }
        throw new IllegalArgumentException("Busca inválida: É obrigatório fornecer userId ou status.");
    }

    private Page<Authorization> executeQuery(String indexName, String partitionValue, String startDate, String endDate,
                                             String filterValue, String filterAttribute, Map<String, AttributeValue> startKey) {

        DynamoDbIndex<Authorization> index = table.index(indexName);
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

        // Aplica filtros que não são chaves de partição/ordenação (Scan secundário dentro do subset de dados da Query)
        if (filterValue != null && filterAttribute != null) {
            Expression filterExpression = Expression.builder()
                    .expression("#attr = :val")
                    .putExpressionName("#attr", filterAttribute)
                    .putExpressionValue(":val", AttributeValue.builder().s(filterValue).build())
                    .build();
            requestBuilder.filterExpression(filterExpression);
        }

        var iterator = index.query(requestBuilder.build()).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }
}