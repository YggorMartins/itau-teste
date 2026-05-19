package dev.itauTeste.itauTeste;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class TransacaoService {

    public void validarTransacao(TransacaoRequest transacaoRequest) {

        // DATA E HORA PRESENTES (BODY NAO PODE SER VAZIO)
        if (transacaoRequest.getValor() == null || transacaoRequest.getDataHora() == null) {
            //transacao nao é valida, valor ou data e hora ausentes
            throw new IllegalArgumentException();

        }

        //VALOR MAIOR OU IGUAL A 0
        if (transacaoRequest.getValor().compareTo(BigDecimal.ZERO) < 0 )  {
            //transacao nao é valida, valor menor ou igual a zero
            throw new IllegalArgumentException("Erro: Isso não é uma transacao valida, transacoes devem ter valor maior do que zero.");

        }

        // DATA MENOR OU IGUAL A DATA DE HOJE
        if (transacaoRequest.getDataHora().isAfter(OffsetDateTime.now())) {
            //transacao nao é valida, data futura
            throw new IllegalArgumentException();

        }

    }
}
