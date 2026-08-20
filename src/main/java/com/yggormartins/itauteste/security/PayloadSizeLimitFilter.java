package com.yggormartins.itauteste.security;

import com.yggormartins.itauteste.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Rejeita Content-Length e transferências chunked acima do limite antes do Jackson. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public final class PayloadSizeLimitFilter extends OncePerRequestFilter {
    private final int maximumBytes;

    public PayloadSizeLimitFilter(AppProperties properties) {
        this.maximumBytes = properties.security().maxPayloadBytes();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getContentLengthLong() == 0
                && !"POST".equalsIgnoreCase(request.getMethod())
                && !"PUT".equalsIgnoreCase(request.getMethod())
                && !"PATCH".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maximumBytes) {
            reject(request, response);
            return;
        }

        // readNBytes limita a alocação a max+1 mesmo quando Content-Length é omitido.
        byte[] body = request.getInputStream().readNBytes(maximumBytes + 1);
        if (body.length > maximumBytes) {
            reject(request, response);
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private static void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SecurityProblemWriter.write(request, response,
                HttpStatus.PAYLOAD_TOO_LARGE.value(), "Payload muito grande",
                "payload_too_large", "O corpo da requisição excede o limite permitido");
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("async reads are not supported");
                }
                @Override public int read() { return input.read(); }
                @Override public int read(byte[] bytes, int off, int len) {
                    return input.read(bytes, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    StandardCharsets.UTF_8));
        }

        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }
    }
}
