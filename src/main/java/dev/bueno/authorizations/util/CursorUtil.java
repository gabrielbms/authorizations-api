package dev.bueno.authorizations.util;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CursorUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Construtor privado para evitar instanciar classe utilitária
    private CursorUtil() {}

    // Serializa o LastEvaluatedKey do DynamoDB em um token Base64 URL-safe (stateless pagination).
    public static String encode(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
            return null;
        }

        try {
            Map<String, String> stringMap = new HashMap<>();
            lastEvaluatedKey.forEach((k, v) -> stringMap.put(k, v.s()));

            byte[] jsonBytes = MAPPER.writeValueAsBytes(stringMap);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar token de paginação.", e);
        }
    }

    // Decodifica o token Base64 de volta para o formato Map<String, AttributeValue> esperado pelo SDK.
    @SuppressWarnings("java:S1168")
    public static Map<String, AttributeValue> decode(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(token);
            Map<String, String> stringMap = MAPPER.readValue(decodedBytes, new TypeReference<>() {});

            Map<String, AttributeValue> attrMap = new HashMap<>();
            stringMap.forEach((k, v) -> attrMap.put(k, AttributeValue.builder().s(v).build()));
            return attrMap;
        } catch (Exception e) {
            throw new IllegalArgumentException("Token de paginação inválido.");
        }
    }
}