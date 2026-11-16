/*
 * ======================================================================================
 * Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
 * Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
 * Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
 *                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
 *                      PostgreSQL database" (student prompt PA2-01, verbatim in PA2/.ai_logs/pa2-session-log.md)
 * AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
 * Human Modifications: None
 * Verification       : API integration tests (AssetApiIntegrationTest, TicketApiIntegrationTest) + docker compose smoke test in CI
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed all ticket endpoints, anyone can approve or allocate
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.web;

import edu.bu.cs763.helpdesk.domain.Allocation;
import edu.bu.cs763.helpdesk.domain.Ticket;
import edu.bu.cs763.helpdesk.domain.TicketStatus;
import edu.bu.cs763.helpdesk.service.TicketService;
import edu.bu.cs763.helpdesk.web.dto.AllocateRequest;
import edu.bu.cs763.helpdesk.web.dto.TicketDetails;
import edu.bu.cs763.helpdesk.web.dto.TicketRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<Ticket> create(@RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request));
    }

    @GetMapping
    public List<Ticket> search(@RequestParam(required = false) TicketStatus status,
                               @RequestParam(required = false) String courseId,
                               @RequestParam(required = false) String labId,
                               @RequestParam(required = false) String projectName,
                               @RequestParam(required = false) UUID assetId) {
        return ticketService.search(status, courseId, labId, projectName, assetId);
    }

    @GetMapping("/{ticketId}")
    public TicketDetails get(@PathVariable UUID ticketId) {
        return ticketService.details(ticketId);
    }

    @PutMapping("/{ticketId}")
    public Ticket update(@PathVariable UUID ticketId, @RequestBody TicketRequest request) {
        return ticketService.update(ticketId, request);
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> delete(@PathVariable UUID ticketId) {
        ticketService.delete(ticketId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ticketId}/allocate")
    public ResponseEntity<List<Allocation>> allocate(@PathVariable UUID ticketId,
                                                     @RequestBody(required = false) AllocateRequest request) {
        List<Allocation> allocations = ticketService.allocate(ticketId, request == null ? null : request.allocatedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(allocations);
    }
}
