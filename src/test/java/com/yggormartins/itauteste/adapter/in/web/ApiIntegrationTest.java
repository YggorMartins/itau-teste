package com.yggormartins.itauteste.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.rate-limit.capacity=1000",
        "app.security.rate-limit.refill-tokens=1000"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clearWindow() throws Exception {
        mockMvc.perform(delete("/transacao")).andExpect(status().isOk());
    }

    @Test
    void createsTransactionAndReturnsStatistics() throws Exception {
        String now = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1).toString();
        mockMvc.perform(post("/transacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10.25,\"dataHora\":\"" + now + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        mockMvc.perform(get("/estatistica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.sum", is(10.25)))
                .andExpect(jsonPath("$.min", is(10.25)))
                .andExpect(jsonPath("$.max", is(10.25)));
    }

    @Test
    void distinguishesMalformedJsonFromBusinessViolations() throws Exception {
        mockMvc.perform(post("/transacao")
                        .contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code", is("malformed_json")))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));

        String now = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1).toString();
        mockMvc.perform(post("/transacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":0,\"dataHora\":\"" + now + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("non_positive_amount")));

        mockMvc.perform(post("/transacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("validation_failed")));
    }

    @Test
    void rejectsFutureExpiredUnknownAndOversizedInputs() throws Exception {
        String future = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1).toString();
        String expired = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2).toString();

        mockMvc.perform(post("/transacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":1,\"dataHora\":\"" + future + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("future_transaction")));
        mockMvc.perform(post("/transacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":1,\"dataHora\":\"" + expired + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("expired_transaction")));
        mockMvc.perform(post("/transacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":1,\"dataHora\":\"" + expired
                                + "\",\"admin\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("malformed_json")));

        mockMvc.perform(post("/transacao").contentType(MediaType.APPLICATION_JSON)
                        .content(" ".repeat(10_241)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code", is("payload_too_large")));
    }

    @Test
    void appliesSecurityHeadersAndCorrelationId() throws Exception {
        mockMvc.perform(get("/estatistica").header("X-Correlation-ID", "test-correlation"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "test-correlation"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'none'")))
                // HSTS é corretamente emitido apenas em conexões HTTPS.
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }
}
