/*
 * ======================================================================================
 * Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
 * Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
 * Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
 *                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
 *                      PostgreSQL database" (student prompt PA2-01, verbatim in .ai_logs/pa2-session-log.md)
 * AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
 * Human Modifications: None
 * Verification       : Executed in CI (mvn -B verify) as the API integration test suite
 *                      Human review: PENDING (student to sign off: name, date, what was checked)
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.bu.cs763.helpdesk.domain.Allocation;
import edu.bu.cs763.helpdesk.domain.AllocationStatus;
import edu.bu.cs763.helpdesk.service.AllocationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Integration tests for the asset endpoints. */
class AssetApiIntegrationTest extends ApiTestSupport {

    @Autowired
    private AllocationService allocationService;

    // ---- POST /api/v1/assets ----
    @Test
    void createAssetDefaultsAvailableStockAndStatus() throws Exception {
        String id = createLaptop(5);
        mockMvc.perform(get("/api/v1/assets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Laptop"))
                .andExpect(jsonPath("$.totalStock").value(5))
                .andExpect(jsonPath("$.availableStock").value(5))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.maxCheckoutDays").value(7));
    }

    // ---- GET /api/v1/assets (multi-field search) ----
    @Test
    void searchFiltersByCategoryProgramStatusAndCourse() throws Exception {
        createLaptop(3);
        createVoucher(1000);
        createLicense(10);

        mockMvc.perform(get("/api/v1/assets").param("category", "LAPTOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("LAPTOP"));

        mockMvc.perform(get("/api/v1/assets").param("departmentProgram", "MET-CS").param("status", "AVAILABLE"))
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/api/v1/assets").param("courseId", "CS763-F26"))
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/v1/assets").param("labId", "LAB-SEC").param("q", "ide"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("SOFTWARE_LICENSE"));
    }

    // ---- GET/PUT/DELETE /api/v1/assets/{asset_id} ----
    @Test
    void readUpdateDeleteAsset() throws Exception {
        String id = createLaptop(2);

        mockMvc.perform(put("/api/v1/assets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Renamed Laptop", "totalStock", 4, "availableStock", 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Laptop"))
                .andExpect(jsonPath("$.totalStock").value(4))
                .andExpect(jsonPath("$.category").value("LAPTOP"));

        mockMvc.perform(delete("/api/v1/assets/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/assets/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void unknownAssetReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/assets/{asset_id}/return ----
    @Test
    void returnCheckedOutDeviceRestoresStockAndRecordsTimestamp() throws Exception {
        String laptop = createLaptop(1);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        approve(ticket);
        String allocations = allocate(ticket);
        String allocationId = JsonPath.read(allocations, "$[0].id");

        mockMvc.perform(get("/api/v1/assets/" + laptop))
                .andExpect(jsonPath("$.availableStock").value(0))
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        mockMvc.perform(post("/api/v1/assets/" + laptop + "/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("allocationId", allocationId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty())
                .andExpect(jsonPath("$.checkoutDurationHours").value(0));

        mockMvc.perform(get("/api/v1/assets/" + laptop))
                .andExpect(jsonPath("$.availableStock").value(1))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // second return of the same allocation is rejected
        mockMvc.perform(post("/api/v1/assets/" + laptop + "/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("allocationId", allocationId))))
                .andExpect(status().isConflict());
    }

    @Test
    void returningNonPhysicalAssetIsRejected() throws Exception {
        String voucher = createVoucher(100);
        mockMvc.perform(post("/api/v1/assets/" + voucher + "/return"))
                .andExpect(status().isConflict());
    }

    // ---- GET /api/v1/assets/allocations ----
    @Test
    void allocationsListingShowsMetricsAndKeysForCourseOrLab() throws Exception {
        String laptop = createLaptop(5);
        String license = createLicense(10);
        String voucher = createVoucher(500);
        String ticket = createTicket(List.of(
                Map.of("assetId", laptop, "quantity", 2),
                Map.of("assetId", license, "quantity", 3),
                Map.of("assetId", voucher, "quantity", 1, "voucherAmount", 120.50)));
        approve(ticket);
        allocate(ticket);

        mockMvc.perform(get("/api/v1/assets/allocations").param("courseId", "CS763-F26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeAllocations").value(3))
                .andExpect(jsonPath("$.checkedOutUnits").value(2))
                .andExpect(jsonPath("$.overdueUnits").value(0))
                .andExpect(jsonPath("$.voucherCreditAllocated").value(120.50))
                .andExpect(jsonPath("$.assignedSoftwareKeys", hasItem("TEST-KEY-1234-ABCD")))
                .andExpect(jsonPath("$.allocations", hasSize(3)));

        mockMvc.perform(get("/api/v1/assets/allocations").param("labId", "LAB-OTHER"))
                .andExpect(jsonPath("$.activeAllocations").value(0));
    }

    // ---- overdue tracking (physical check-out duration rules) ----
    @Test
    void pastDueCheckoutIsFlaggedOverdue() throws Exception {
        String laptop = createLaptop(2);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        approve(ticket);
        allocate(ticket);

        Allocation allocation = allocationRepository.findAll().get(0);
        allocation.setDueAt(Instant.now().minus(1, ChronoUnit.DAYS));
        allocationRepository.save(allocation);

        allocationService.markOverdue();

        Allocation reloaded = allocationRepository.findById(allocation.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(AllocationStatus.OVERDUE, reloaded.getStatus());
        mockMvc.perform(get("/api/v1/assets/" + laptop)).andExpect(jsonPath("$.status").value("OVERDUE"));
        mockMvc.perform(get("/api/v1/assets/allocations").param("courseId", "CS763-F26"))
                .andExpect(jsonPath("$.overdueUnits").value(1));
    }
}
