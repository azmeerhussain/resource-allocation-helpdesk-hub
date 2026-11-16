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
 *                      Human review: PENDING (student to sign off: name, date, what was checked)
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Integration tests for the help desk ticket endpoints and the allocation engine. */
class TicketApiIntegrationTest extends ApiTestSupport {

    // ---- POST /api/v1/assets/tickets ----
    @Test
    void submitTicketStartsPendingWithItems() throws Exception {
        String laptop = createLaptop(3);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 2)));

        mockMvc.perform(get("/api/v1/assets/tickets/" + ticket))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("PENDING"))
                .andExpect(jsonPath("$.ticket.items", hasSize(1)))
                .andExpect(jsonPath("$.ticket.items[0].quantity").value(2))
                .andExpect(jsonPath("$.history", hasSize(1)))
                .andExpect(jsonPath("$.metrics.itemCount").value(1))
                .andExpect(jsonPath("$.metrics.totalQuantityRequested").value(2));
    }

    // ---- GET /api/v1/assets/tickets (filters) ----
    @Test
    void listTicketsFiltersByStatusCourseAndProject() throws Exception {
        String laptop = createLaptop(5);
        String first = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        approve(first);

        mockMvc.perform(get("/api/v1/assets/tickets").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/assets/tickets").param("status", "APPROVED"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(first));
        mockMvc.perform(get("/api/v1/assets/tickets").param("courseId", "CS763-F26").param("projectName", "Capstone A"))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/v1/assets/tickets").param("assetId", laptop))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/v1/assets/tickets").param("courseId", "NOPE"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---- PUT /api/v1/assets/tickets/{ticket_id} (content update) ----
    @Test
    void updatePendingTicketContents() throws Exception {
        String laptop = createLaptop(5);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));

        mockMvc.perform(put("/api/v1/assets/tickets/" + ticket)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("justification", "Need more units",
                                "items", List.of(Map.of("assetId", laptop, "quantity", 4))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.justification").value("Need more units"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    // ---- PUT /api/v1/assets/tickets/{ticket_id} (approval state transition) ----
    @Test
    void approvalDoesNotTouchInventoryButReservesAsset() throws Exception {
        String laptop = createLaptop(3);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 2)));

        mockMvc.perform(put("/api/v1/assets/tickets/" + ticket)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "APPROVED", "reviewedBy", "IT Director"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("IT Director"))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/assets/" + laptop))
                .andExpect(jsonPath("$.availableStock").value(3))
                .andExpect(jsonPath("$.status").value("RESERVED"));

        // approved tickets can no longer be edited
        mockMvc.perform(put("/api/v1/assets/tickets/" + ticket)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("justification", "changed"))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectAndCloseTicket() throws Exception {
        String laptop = createLaptop(1);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));

        mockMvc.perform(put("/api/v1/assets/tickets/" + ticket)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "REJECTED", "reviewedBy", "IT Director", "reviewNote", "no budget"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(put("/api/v1/assets/tickets/" + ticket)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "CLOSED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mockMvc.perform(get("/api/v1/assets/tickets/" + ticket))
                .andExpect(jsonPath("$.metrics.statusChanges").value(3));
    }

    @Test
    void cannotJumpToAllocatedThroughPut() throws Exception {
        String laptop = createLaptop(1);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        mockMvc.perform(put("/api/v1/assets/tickets/" + ticket)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ALLOCATED"))))
                .andExpect(status().isConflict());
    }

    // ---- DELETE /api/v1/assets/tickets/{ticket_id} ----
    @Test
    void deletePendingTicketOnly() throws Exception {
        String laptop = createLaptop(2);
        String pending = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        String approved = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        approve(approved);

        mockMvc.perform(delete("/api/v1/assets/tickets/" + pending)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/assets/tickets/" + pending)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/assets/tickets/" + approved)).andExpect(status().isConflict());
    }

    // ---- POST /api/v1/assets/tickets/{ticket_id}/allocate ----
    @Test
    void allocateApprovedTicketDeductsInventoryAndIssuesTokens() throws Exception {
        String laptop = createLaptop(5);
        String license = createLicense(10);
        String voucher = createVoucher(1000);
        String ticket = createTicket(List.of(
                Map.of("assetId", laptop, "quantity", 2),
                Map.of("assetId", license, "quantity", 3),
                Map.of("assetId", voucher, "quantity", 1, "voucherAmount", 250)));
        approve(ticket);

        mockMvc.perform(post("/api/v1/assets/tickets/" + ticket + "/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("allocatedBy", "IT Admin"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.category == 'LAPTOP')].issuedToken", everyItem(startsWith("HW-"))))
                .andExpect(jsonPath("$[?(@.category == 'LAPTOP')].dueAt", hasSize(1)))
                .andExpect(jsonPath("$[?(@.category == 'SOFTWARE_LICENSE')].issuedToken", hasItem("TEST-KEY-1234-ABCD")))
                .andExpect(jsonPath("$[?(@.category == 'CLOUD_VOUCHER')].issuedToken", everyItem(startsWith("VCH-"))));

        mockMvc.perform(get("/api/v1/assets/" + laptop)).andExpect(jsonPath("$.availableStock").value(3));
        mockMvc.perform(get("/api/v1/assets/" + license)).andExpect(jsonPath("$.availableStock").value(7));
        mockMvc.perform(get("/api/v1/assets/" + voucher)).andExpect(jsonPath("$.voucherBalance").value(750.0));
        mockMvc.perform(get("/api/v1/assets/tickets/" + ticket))
                .andExpect(jsonPath("$.ticket.status").value("ALLOCATED"))
                .andExpect(jsonPath("$.ticket.allocatedBy").value("IT Admin"))
                .andExpect(jsonPath("$.allocations", hasSize(3)));

        // allocating twice is rejected
        mockMvc.perform(post("/api/v1/assets/tickets/" + ticket + "/allocate")).andExpect(status().isConflict());
    }

    @Test
    void pendingTicketCannotBeAllocated() throws Exception {
        String laptop = createLaptop(5);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 1)));
        mockMvc.perform(post("/api/v1/assets/tickets/" + ticket + "/allocate")).andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/assets/" + laptop)).andExpect(jsonPath("$.availableStock").value(5));
    }

    @Test
    void voucherOverdraftIsRejectedAndWholeAllocationRollsBack() throws Exception {
        String laptop = createLaptop(5);
        String voucher = createVoucher(100);
        String ticket = createTicket(List.of(
                Map.of("assetId", laptop, "quantity", 2),
                Map.of("assetId", voucher, "quantity", 1, "voucherAmount", 150)));
        approve(ticket);

        mockMvc.perform(post("/api/v1/assets/tickets/" + ticket + "/allocate")).andExpect(status().isConflict());

        // nothing changed: laptop stock and voucher balance intact, ticket still APPROVED
        mockMvc.perform(get("/api/v1/assets/" + laptop)).andExpect(jsonPath("$.availableStock").value(5));
        mockMvc.perform(get("/api/v1/assets/" + voucher)).andExpect(jsonPath("$.voucherBalance").value(100.0));
        mockMvc.perform(get("/api/v1/assets/tickets/" + ticket))
                .andExpect(jsonPath("$.ticket.status").value("APPROVED"))
                .andExpect(jsonPath("$.allocations", hasSize(0)));
    }

    @Test
    void insufficientStockIsRejected() throws Exception {
        String laptop = createLaptop(1);
        String ticket = createTicket(List.of(Map.of("assetId", laptop, "quantity", 3)));
        approve(ticket);
        mockMvc.perform(post("/api/v1/assets/tickets/" + ticket + "/allocate")).andExpect(status().isConflict());
    }
}
