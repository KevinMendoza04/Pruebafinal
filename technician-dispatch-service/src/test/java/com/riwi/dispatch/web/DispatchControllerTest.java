package com.riwi.dispatch.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riwi.dispatch.web.filter.TraceIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Spring context integration test (no containers needed — no DB).
 * Validates the HTTP contract, error bodies, and traceId header behaviour.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DispatchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /technician-dispatch 200 — correct JSON field names and types")
    void happyPath_200() throws Exception {
        String body = """
                {"ticketId":125,"categoria":"RED","prioridad":"ALTA"}
                """;

        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(125))
                .andExpect(jsonPath("$.tecnicoId").value(matchesPattern("TEC-\\d{3}")))
                .andExpect(jsonPath("$.disponibilidad").value(anyOf(is("ALTA"), is("MEDIA"), is("BAJA"))))
                .andExpect(jsonPath("$.tiempoEstimadoHoras").isNumber());
    }

    @Test
    @DisplayName("POST /technician-dispatch is deterministic — two identical requests return identical bodies")
    void happyPath_deterministic() throws Exception {
        String body = """
                {"ticketId":125,"categoria":"RED","prioridad":"ALTA"}
                """;

        String response1 = mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Bodies must be byte-for-byte identical (deterministic algorithm)
        org.assertj.core.api.Assertions.assertThat(response1).isEqualTo(response2);
    }

    // ── TraceId header ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Response always contains X-Trace-Id header")
    void traceId_presentInResponse() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":1,"categoria":"HARDWARE","prioridad":"BAJA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.TRACE_HEADER));
    }

    @Test
    @DisplayName("Valid X-Trace-Id from request is echoed back")
    void traceId_validIncomingIsEchoed() throws Exception {
        String myTrace = "abc-12345678";
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TraceIdFilter.TRACE_HEADER, myTrace)
                        .content("""
                                {"ticketId":1,"categoria":"HARDWARE","prioridad":"BAJA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_HEADER, myTrace));
    }

    @Test
    @DisplayName("Invalid X-Trace-Id (too short) is replaced with a generated UUID")
    void traceId_invalidIncomingIsReplaced() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TraceIdFilter.TRACE_HEADER, "bad")   // only 3 chars → invalid
                        .content("""
                                {"ticketId":1,"categoria":"HARDWARE","prioridad":"BAJA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_HEADER, not("bad")));
    }

    // ── 400 — missing field ────────────────────────────────────────────────────

    @Test
    @DisplayName("400 when ticketId is missing")
    void error_400_missingTicketId() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoria":"RED","prioridad":"ALTA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("400 when categoria is missing")
    void error_400_missingCategoria() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":1,"prioridad":"ALTA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("400 when prioridad is missing")
    void error_400_missingPrioridad() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":1,"categoria":"RED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── 400 — invalid enum ────────────────────────────────────────────────────

    @Test
    @DisplayName("400 when categoria has unknown enum value")
    void error_400_unknownCategoria() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":1,"categoria":"UNKNOWN","prioridad":"ALTA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("400 when prioridad has unknown enum value")
    void error_400_unknownPrioridad() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":1,"categoria":"RED","prioridad":"URGENTE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── 400 — negative ticketId ────────────────────────────────────────────────

    @Test
    @DisplayName("400 when ticketId is negative")
    void error_400_negativeTicketId() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":-5,"categoria":"RED","prioridad":"ALTA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("400 when ticketId is zero")
    void error_400_zeroTicketId() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":0,"categoria":"RED","prioridad":"ALTA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── 400 — malformed JSON ──────────────────────────────────────────────────

    @Test
    @DisplayName("400 when body is not valid JSON")
    void error_400_malformedJson() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-a-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.type").value(containsString("helpflow.local")));
    }

    // ── ProblemDetail shape ────────────────────────────────────────────────────

    @Test
    @DisplayName("Error body has all required ProblemDetail fields")
    void errorBody_hasAllRequiredFields() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }
}
