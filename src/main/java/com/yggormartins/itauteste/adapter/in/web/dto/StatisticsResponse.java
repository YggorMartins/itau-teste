package com.yggormartins.itauteste.adapter.in.web.dto;

import com.yggormartins.itauteste.domain.model.Statistics;

public record StatisticsResponse(long count, double sum, double avg,
                                 double min, double max) {
    public static StatisticsResponse from(Statistics statistics) {
        // O domínio preserva BigDecimal; esta conversão existe só pelo contrato legado.
        return new StatisticsResponse(statistics.count(),
                statistics.sum().doubleValue(),
                statistics.average().doubleValue(),
                statistics.minimum().doubleValue(),
                statistics.maximum().doubleValue());
    }
}
