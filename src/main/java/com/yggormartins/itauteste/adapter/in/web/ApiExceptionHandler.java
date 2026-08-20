package com.yggormartins.itauteste.adapter.in.web;

import com.yggormartins.itauteste.domain.exception.InvalidTransactionException;
import com.yggormartins.itauteste.security.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.Map;

@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String PROBLEM_BASE = "https://api.example.com/problems/";

    @ExceptionHandler(InvalidTransactionException.class)
    ResponseEntity<ProblemDetail> handleInvalidTransaction(
            InvalidTransactionException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY,
                "Transação inválida", exception.code(), exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "Requisição inválida", "malformed_json",
                "JSON ausente, malformado ou com campo/tipo não permitido",
                request.getDescription(false).replaceFirst("^uri=", ""));
        return handleExceptionInternal(exception, problem, headers,
                HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY,
                "Falha de validação", "validation_failed",
                "Um ou mais campos não atendem ao contrato",
                request.getDescription(false).replaceFirst("^uri=", ""));
        problem.setProperty("violations", exception.getBindingResult().getFieldErrors()
                .stream().map(error -> Map.of(
                        "field", error.getField(),
                        "message", "campo inválido")).toList());
        return handleExceptionInternal(exception, problem, headers,
                HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception,
                                                    HttpServletRequest request) {
        // Não registra mensagem/payload potencialmente sensível e nunca envia stacktrace ao cliente.
        LOG.error("Unhandled exception type={} correlationId={}",
                exception.getClass().getName(), correlationId());
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno", "internal_error",
                "A requisição não pôde ser processada", request.getRequestURI());
        return ResponseEntity.internalServerError().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String title,
                                         String code, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(PROBLEM_BASE + code));
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", correlationId());
        return problem;
    }

    private static String correlationId() {
        String value = MDC.get(CorrelationIdFilter.MDC_KEY);
        return value == null ? "unavailable" : value;
    }
}
