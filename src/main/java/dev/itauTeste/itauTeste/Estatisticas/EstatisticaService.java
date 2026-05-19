package dev.itauTeste.itauTeste.Estatisticas;

import dev.itauTeste.itauTeste.EstatisticaDTO;
import dev.itauTeste.itauTeste.TransacaoRepository;
import dev.itauTeste.itauTeste.TransacaoRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;

@Service
public class EstatisticaService {

    @Autowired
    private TransacaoRepository repository;

    public EstatisticaDTO calcularEstatisticas() {
        OffsetDateTime limite = OffsetDateTime.now().minusSeconds(60);
        List<TransacaoRequest> transacoes = repository.buscarTodas();

        // Filtra as transações dos últimos 60 segundos e converte para Double
        DoubleSummaryStatistics stats = transacoes.stream()
                .filter(t -> t.getDataHora().isAfter(limite))
                .mapToDouble(t -> t.getValor().doubleValue())
                .summaryStatistics();

        // Se não houver transações no período, o count será 0 e os valores 0.0
        if (stats.getCount() == 0) {
            return new EstatisticaDTO(0, 0.0, 0.0, 0.0, 0.0);
        }

        return new EstatisticaDTO(
                stats.getCount(),
                stats.getSum(),
                stats.getAverage(),
                stats.getMin(),
                stats.getMax()
        );
    }
}