package com.yggormartins.itauteste.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;

/** Snapshot monetário coerente; nenhuma conta de domínio usa ponto flutuante. */
public record Statistics(long count, BigDecimal sum, BigDecimal average,
                         BigDecimal minimum, BigDecimal maximum) {
    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    public static Statistics empty() {
        return new Statistics(0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static Statistics of(long count, BigDecimal sum,
                                BigDecimal minimum, BigDecimal maximum) {
        if (count == 0) {
            return empty();
        }
        return new Statistics(count, sum,
                sum.divide(BigDecimal.valueOf(count), MATH_CONTEXT),
                minimum, maximum);
    }
}
