package dev.bueno.authorizations.domain;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public record DateRange(String start, String end) {

    public DateRange {
        validate(start, "startDate");
        validate(end, "endDate");

        // Valida se o início é posterior ao fim
        if (start != null && end != null && Instant.parse(start).isAfter(Instant.parse(end))) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data final.");
        }
    }

    private void validate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.isBlank()) {
            return;
        }
        try {
            // Apenas para checar se o formato está correto
            Instant.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de data inválido para " + fieldName + ". Esperado formato ISO-8601 (e.g., 2026-01-01T00:00:00Z)");
        }
    }
}