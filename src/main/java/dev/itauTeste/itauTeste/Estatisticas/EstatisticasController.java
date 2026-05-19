package dev.itauTeste.itauTeste.Estatisticas;

import dev.itauTeste.itauTeste.EstatisticaDTO;
import dev.itauTeste.itauTeste.Estatisticas.EstatisticaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estatistica")
public class EstatisticasController {

    @Autowired
    private EstatisticaService estatisticaService;

    @GetMapping
    public ResponseEntity<EstatisticaDTO> obterEstatisticas() {
        // Agora o método será encontrado
        return ResponseEntity.ok(estatisticaService.calcularEstatisticas());
    }
}