package dev.itauTeste.itauTeste;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransacaoRepository {

    List<TransacaoRequest> listaDeTransacoes = new ArrayList<>();

    // salvar os dados em lista
    public void salvarDados(TransacaoRequest transacaoRequest) {
        listaDeTransacoes.add(transacaoRequest);
    }

    public List<TransacaoRequest> buscarTodas() {
        return this.listaDeTransacoes;
    }

    // apagar lista depois de 60sec
    @Scheduled(fixedRate = 60000)
    public void limparDados(){
        OffsetDateTime limite = OffsetDateTime.now().minusSeconds(60);

        listaDeTransacoes.removeIf(transacao -> transacao.getDataHora().isBefore(limite));
    }

    // apagar todas as transaçoes da lista
    public void deletarDados() {
        listaDeTransacoes.clear();
    }
}
