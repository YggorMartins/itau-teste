package com.yggormartins.itauteste.config;

import com.yggormartins.itauteste.adapter.out.memory.AtomicRingTransactionWindow;
import com.yggormartins.itauteste.application.port.out.TransactionWindow;
import com.yggormartins.itauteste.application.service.TransactionApplicationService;
import com.yggormartins.itauteste.domain.service.TransactionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionPolicy transactionPolicy(Clock clock, AppProperties properties) {
        return new TransactionPolicy(clock, properties.statisticsWindow());
    }

    @Bean
    TransactionWindow transactionWindow(AppProperties properties) {
        return new AtomicRingTransactionWindow(properties.statisticsWindow());
    }

    @Bean
    TransactionApplicationService transactionApplicationService(
            TransactionPolicy policy, TransactionWindow window, Clock clock) {
        return new TransactionApplicationService(policy, window, clock);
    }
}
