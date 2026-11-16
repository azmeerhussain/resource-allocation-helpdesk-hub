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
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed all asset endpoints, no check on who is calling
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.web;

import edu.bu.cs763.helpdesk.domain.Allocation;
import edu.bu.cs763.helpdesk.domain.Asset;
import edu.bu.cs763.helpdesk.domain.AssetCategory;
import edu.bu.cs763.helpdesk.domain.AssetStatus;
import edu.bu.cs763.helpdesk.service.AllocationService;
import edu.bu.cs763.helpdesk.service.AssetService;
import edu.bu.cs763.helpdesk.web.dto.AllocationSummary;
import edu.bu.cs763.helpdesk.web.dto.AssetRequest;
import edu.bu.cs763.helpdesk.web.dto.ReturnRequest;
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
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;
    private final AllocationService allocationService;

    public AssetController(AssetService assetService, AllocationService allocationService) {
        this.assetService = assetService;
        this.allocationService = allocationService;
    }

    @PostMapping
    public ResponseEntity<Asset> create(@RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
    }

    @GetMapping
    public List<Asset> search(@RequestParam(required = false) AssetStatus status,
                              @RequestParam(required = false) AssetCategory category,
                              @RequestParam(required = false) String departmentProgram,
                              @RequestParam(required = false) String courseId,
                              @RequestParam(required = false) String labId,
                              @RequestParam(required = false) String q) {
        return assetService.search(status, category, departmentProgram, courseId, labId, q);
    }

    @GetMapping("/allocations")
    public AllocationSummary allocations(@RequestParam(required = false) String courseId,
                                         @RequestParam(required = false) String labId) {
        return allocationService.activeFor(courseId, labId);
    }

    @GetMapping("/{assetId}")
    public Asset get(@PathVariable UUID assetId) {
        return assetService.get(assetId);
    }

    @PutMapping("/{assetId}")
    public Asset update(@PathVariable UUID assetId, @RequestBody AssetRequest request) {
        return assetService.update(assetId, request);
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> delete(@PathVariable UUID assetId) {
        assetService.delete(assetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{assetId}/return")
    public Allocation returnAsset(@PathVariable UUID assetId,
                                  @RequestBody(required = false) ReturnRequest request) {
        return assetService.returnAsset(assetId, request == null ? null : request.allocationId());
    }
}
