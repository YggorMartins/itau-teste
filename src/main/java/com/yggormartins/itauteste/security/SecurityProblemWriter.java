package com.yggormartins.itauteste.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Escreve somente mensagens constantes e IDs previamente validados. */
public final class SecurityProblemWriter {
    private SecurityProblemWriter() {
    }

    public static void write(HttpServletRequest request, HttpServletResponse response,
                             int status, String title, String code, String detail)
            throws IOException {
        response.resetBuffer();
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/problem+json");
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        String safeCorrelationId = correlationId == null ? "unavailable" : correlationId;
        // title/code/detail são constantes internas; requestURI recebe escape mínimo JSON.
        String instance = escapeJson(request.getRequestURI());
        response.getWriter().write("{\"type\":\"https://api.example.com/problems/"
                + code + "\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\",\"instance\":\"" + instance
                + "\",\"code\":\"" + code + "\",\"correlationId\":\""
                + safeCorrelationId + "\"}");
        response.flushBuffer();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "").replace("\n", "");
    }
}
