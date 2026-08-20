package com.yggormartins.itauteste.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Identificadores sensíveis são reduzidos a SHA-256 antes de entrar no cache. */
@Component
public final class ClientIdentityResolver {
    public String resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "subject:" + sha256(authentication.getName());
        }

        // Não confia diretamente em X-Forwarded-For, que pode ser forjado.
        // Também não confia em X-API-Key sem uma autenticação prévia: aceitar um
        // valor arbitrário permitiria ao atacante rotacionar chaves e burlar o limite.
        // Em produção, o proxy confiável deve normalizar remoteAddr; tokens/API keys
        // válidos chegam aqui como Authentication e são limitados pelo subject.
        return "ip:" + request.getRemoteAddr();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
