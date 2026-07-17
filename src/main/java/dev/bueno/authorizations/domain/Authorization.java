package dev.bueno.authorizations.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbBean
public class Authorization {

    private String authorizationId;
    private String userId;
    private String status;
    private String createdAt;

    public Authorization() {
        // Construtor vazio obrigatório para o SDK do DynamoDB
    }

    @DynamoDbPartitionKey
    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"user-date-index"})
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"status-date-index"})
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // A data de criação atua como Sort Key (Chave de Ordenação/Busca) para ambos os índices secundários
    @DynamoDbSecondarySortKey(indexNames = {"user-date-index", "status-date-index"})
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}