package com.example.projetogroq.exception.global;

import com.example.projetogroq.exception.*;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Responsável por criar todas as exceptions response usando a mesma base
    private ResponseEntity<ExceptionResponse> buildErrorResponse(
            HttpStatus status, String message, Exception e, String logMessage, Object... logArgs) {

        if (logArgs.length > 0) {
            logger.error(logMessage, logArgs);
        } else {
            logger.error(logMessage, e.getMessage());
        }
        logger.error("Exception path: ", e);

        ExceptionResponse er = new ExceptionResponse(
                status.value(),
                status.getReasonPhrase(),
                message
        );

        return ResponseEntity.status(status).body(er);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(Exception e) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error has occurred.",
                e,
                "Unexpected exception happened: {}"
        );
    }

    @ExceptionHandler(GroqTooManyAttempsException.class)
    public ResponseEntity<ExceptionResponse> handleFailedGeneratedJson(GroqTooManyAttempsException e) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "External API was unable to respond correctly.",
                e,
                "Groq API could not send correct JSON Response, causing failed_generation: {}"
        );
    }

    @ExceptionHandler(PresentationTemplateNotFoundIOException.class)
    public ResponseEntity<ExceptionResponse> handleTemplateFileNotFound(PresentationTemplateNotFoundIOException e) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Could not find placeholder file",
                e,
                "The template does not exist or is not in the correct location: {}"
        );
    }

    @ExceptionHandler(IllegalPresentationStateException.class)
    public ResponseEntity<ExceptionResponse> handleNoPresentationInSession(IllegalPresentationStateException e) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "No presentation was created.",
                e,
                "The session started but there isn't any attached presentation: {}"
        );
    }

    @ExceptionHandler(IllegalSessionStateException.class)
    public ResponseEntity<ExceptionResponse> handleSessionNonExistent(IllegalSessionStateException e) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "The session has not yet started.",
                e,
                "The session was not initiated: {}"
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ExceptionResponse> handleUnexpectedExternalApiException(IllegalStateException e) {
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "Unexpected api response",
                e,
                "Unexpected response from External API: {}"
        );
    }

    @ExceptionHandler(GroqIllegalResponseException.class)
    public ResponseEntity<ExceptionResponse> handleNoResponseFromExternalApi(GroqIllegalResponseException e) {
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "Unexpected api response",
                e,
                "Groq returned no response: {}"
        );
    }

    @ExceptionHandler(GroqResponseParseException.class)
    public ResponseEntity<ExceptionResponse> handleParsingExceptionFromApiResponse(GroqResponseParseException e) {
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "Parse error.",
                e,
                "Couldn't parse json received from Groq: {}"
        );
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ExceptionResponse> handleWebClientResponseException(WebClientResponseException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String message;
        String logMessage;
        Object[] logArgs;

        // Tratamento específico para 401/403
        boolean isUnauthorized = e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED);
        boolean isForbidden = e.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN);

        if (isUnauthorized || isForbidden) {
            message = "External service authentication failed";
            logMessage = "Authentication failed with external API - Status: {}, Headers: {}";
            logArgs = new Object[]{e.getStatusCode(), sanitizeHeaders(e.getHeaders())};
        } else {
            message = "External API error";
            logMessage = "External API returned error - Status: {}, Response: {}";
            logArgs = new Object[]{e.getStatusCode(), e.getResponseBodyAsString()};
        }

        logger.error(logMessage, logArgs);
        logger.error("Exception path: ", e);

        ExceptionResponse er = new ExceptionResponse(
                status.value(),
                status.getReasonPhrase(),
                message
        );

        return ResponseEntity.status(status).body(er);
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ExceptionResponse> handleWebClientRequestException(WebClientRequestException e) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "External service unavailable",
                e,
                "Failed to connect to external API: {} - URI: {}",
                e.getMessage(), e.getUri()
        );
    }

    @ExceptionHandler(ReadTimeoutException.class)
    public ResponseEntity<ExceptionResponse> handleReadTimeout(ReadTimeoutException e) {
        return buildErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT,
                "External API timeout",
                e,
                "Timeout when calling external API: {}"
        );
    }

    @ExceptionHandler(WebClientException.class)
    public ResponseEntity<ExceptionResponse> handleGenericWebClientException(WebClientException e) {
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "External communication error",
                e,
                "Unexpected WebClient error: {}"
        );
    }

    private HttpHeaders sanitizeHeaders(HttpHeaders headers) {
        HttpHeaders sanitized = new HttpHeaders();
        headers.forEach((key, value) -> {
            if (key.toLowerCase().contains("authorization") ||
                    key.toLowerCase().contains("api-key") ||
                    key.toLowerCase().contains("token")) {
                sanitized.add(key, "***");
            } else {
                sanitized.addAll(key, value);
            }
        });
        return sanitized;
    }
}