package dev.bueno.authorizations.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR_KEY, "O corpo da requisição é obrigatório e deve estar no formato JSON válido."));
    }

    // Trata regras de negócio e validações manuais
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(ERROR_KEY, ex.getMessage()));
    }

    // Trata falhas das anotações @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Percorre todos os campos que falharam na validação
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParams(MissingServletRequestParameterException ex) {
        // Retorna uma mensagem amigável indicando qual parâmetro faltou
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR_KEY, "O parâmetro '" + ex.getParameterName() + "' é obrigatório."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "O parâmetro '" + ex.getName() + "' possui um valor inválido.";
        Class<?> requiredType = ex.getRequiredType();

        if (requiredType != null && requiredType.isEnum()) {
            message = "Valor inválido para o campo '" + ex.getName() + "'. Valores aceitos: "
                    + java.util.Arrays.toString(requiredType.getEnumConstants());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(ERROR_KEY, message));
    }

    // Tratamento genérico para não vazar StackTrace em erros desconhecidos
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(ERROR_KEY, "Erro interno no servidor. Contate o administrador."));
    }
}