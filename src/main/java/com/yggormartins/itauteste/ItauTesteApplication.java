package com.yggormartins.itauteste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ItauTesteApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItauTesteApplication.class, args);
    }
}
