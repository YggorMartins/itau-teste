package com.yggormartins.itauteste.domain.exception;

/** Erro de regra de negócio, deliberadamente independente de HTTP/Spring. */
public final class InvalidTransactionException extends RuntimeException {
    private final String code;

    public InvalidTransactionException(String code, String safeMessage) {
        super(safeMessage);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
