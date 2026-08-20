package com.yggormartins.itauteste.adapter.in.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** DTO imutável e fechado; propriedades desconhecidas são rejeitadas globalmente. */
public record CreateTransactionRequest(
        @NotNull @Digits(integer = 15, fraction = 2) BigDecimal valor,
        @NotNull OffsetDateTime dataHora) {
}
