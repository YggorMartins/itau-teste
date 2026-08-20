package com.yggormartins.itauteste.adapter.in.web;

import com.yggormartins.itauteste.adapter.in.web.dto.StatisticsResponse;
import com.yggormartins.itauteste.application.service.TransactionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estatistica")
public final class StatisticsController {
    private final TransactionApplicationService service;

    public StatisticsController(TransactionApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public StatisticsResponse get() {
        return StatisticsResponse.from(service.statistics());
    }
}
