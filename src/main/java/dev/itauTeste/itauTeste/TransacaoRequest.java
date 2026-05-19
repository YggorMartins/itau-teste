package dev.itauTeste.itauTeste;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class TransacaoRequest {
    private BigDecimal valor;
    private OffsetDateTime dataHora;
}
