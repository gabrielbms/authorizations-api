package dev.bueno.authorizations.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CursorUtilTest {

    @Test
    @DisplayName("Encode e Decode: Deve realizar o ciclo de ida e volta preservando os dados")
    void shouldPerformRoundTripSuccessfully() {
        // Arrange
        Map<String, AttributeValue> originalMap = new HashMap<>();
        originalMap.put("userId", AttributeValue.builder().s("user-123").build());
        originalMap.put("createdAt", AttributeValue.builder().s("2026-07-16T20:00:00Z").build());

        // Act
        String token = CursorUtil.encode(originalMap);
        Map<String, AttributeValue> decodedMap = CursorUtil.decode(token);

        // Assert
        assertNotNull(token);
        assertEquals(originalMap, decodedMap, "O mapa decodificado deve ser idêntico ao original.");
    }

    @Test
    @DisplayName("Encode: Deve retornar null para entradas nulas ou vazias")
    void shouldReturnNullOnEncodeWhenInputIsEmpty() {
        assertNull(CursorUtil.encode(null));
        assertNull(CursorUtil.encode(new HashMap<>()));
    }

    @Test
    @DisplayName("Decode: Deve retornar null para tokens nulos ou em branco")
    void shouldReturnNullOnDecodeWhenTokenIsEmpty() {
        assertNull(CursorUtil.decode(null));
        assertNull(CursorUtil.decode(""));
        assertNull(CursorUtil.decode("   "));
    }

    @Test
    @DisplayName("Decode: Deve lançar exceção para formato Base64 inválido")
    void shouldThrowExceptionWhenBase64IsInvalid() {
        String invalidToken = "!!!";
        assertThrows(IllegalArgumentException.class, () -> CursorUtil.decode(invalidToken));
    }

    @Test
    @DisplayName("Decode: Deve lançar exceção quando o conteúdo não for um JSON válido")
    void shouldThrowExceptionWhenJsonIsCorrupted() {
        String invalidJsonToken = "aGVsbG8gd29ybGQ";
        assertThrows(IllegalArgumentException.class, () -> CursorUtil.decode(invalidJsonToken));
    }
}