/*
 * ======================================================================================
 * Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
 * Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
 * Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
 *                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
 *                      PostgreSQL database" (student prompt PA2-01, verbatim in PA2/.ai_logs/pa2-session-log.md)
 * AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
 * Human Modifications: None
 * Verification       : Executed in CI (mvn -B verify) as the API integration test suite
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed helper methods, approves and allocates by just sending a name, so the tests assume anyone can do it
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import edu.bu.cs763.helpdesk.repository.AllocationRepository;
import edu.bu.cs763.helpdesk.repository.AssetRepository;
import edu.bu.cs763.helpdesk.repository.TicketEventRepository;
import edu.bu.cs763.helpdesk.repository.TicketRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Shared setup and helpers for the API integration tests (full Spring context + H2 in PostgreSQL mode). */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AssetRepository assetRepository;

    @Autowired
    protected TicketRepository ticketRepository;

    @Autowired
    protected TicketEventRepository ticketEventRepository;

    @Autowired
    protected AllocationRepository allocationRepository;

    @BeforeEach
    void cleanDatabase() {
        allocationRepository.deleteAll();
        ticketEventRepository.deleteAll();
        ticketRepository.deleteAll();
        assetRepository.deleteAll();
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected String createAsset(Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    protected String createLaptop(int stock) throws Exception {
        return createAsset(Map.of("name", "Test Laptop", "category", "LAPTOP", "totalStock", stock,
                "departmentProgram", "MET-CS", "courseId", "CS763-F26", "maxCheckoutDays", 7));
    }

    protected String createVoucher(double balance) throws Exception {
        return createAsset(Map.of("name", "Cloud Credits", "category", "CLOUD_VOUCHER", "totalStock", 1,
                "departmentProgram", "MET-CS", "courseId", "CS763-F26", "voucherBalance", balance));
    }

    protected String createLicense(int seats) throws Exception {
        return createAsset(Map.of("name", "IDE License", "category", "SOFTWARE_LICENSE", "totalStock", seats,
                "departmentProgram", "MET-CS", "labId", "LAB-SEC", "licenseKey", "TEST-KEY-1234-ABCD"));
    }

    protected String createTicket(List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = Map.of(
                "requesterName", "Prof. Example",
                "requesterRole", "INSTRUCTOR",
                "courseId", "CS763-F26",
                "labId", "LAB-SEC",
                "projectName", "Capstone A",
                "justification", "Hardware for the term project",
                "items", items);
        String response = mockMvc.perform(post("/api/v1/assets/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    protected void approve(String ticketId) throws Exception {
        mockMvc.perform(put("/api/v1/assets/tickets/" + ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "APPROVED", "reviewedBy", "IT Director", "reviewNote", "ok"))))
                .andExpect(status().isOk());
    }

    protected String allocate(String ticketId) throws Exception {
        return mockMvc.perform(post("/api/v1/assets/tickets/" + ticketId + "/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("allocatedBy", "IT Admin"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
