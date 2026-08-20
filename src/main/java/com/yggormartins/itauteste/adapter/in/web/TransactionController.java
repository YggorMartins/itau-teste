package com.yggormartins.itauteste.adapter.in.web;

import com.yggormartins.itauteste.adapter.in.web.dto.CreateTransactionRequest;
import com.yggormartins.itauteste.application.service.TransactionApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transacao")
public final class TransactionController {
    private final TransactionApplicationService service;

    public TransactionController(TransactionApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CreateTransactionRequest request) {
        service.create(request.valor(), request.dataHora().toInstant());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        service.clear();
        return ResponseEntity.ok().build();
    }
}
