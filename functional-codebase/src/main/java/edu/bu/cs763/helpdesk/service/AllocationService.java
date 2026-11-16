/*
 * ======================================================================================
 * Language / Stack   : Java 21 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL 16 · Maven · Docker
 * Tool               : Claude (Anthropic), model claude-opus-5-5, Claude Code-based agentic session
 * Prompt Summary     : "build the Domain 2 service from ProjectPortalDomainServices.docx ... implement
 *                      all ... endpoints and the 5 functional requirements ... Java with Spring Boot and a
 *                      PostgreSQL database" (student prompt PA2-01, verbatim in .ai_logs/pa2-session-log.md)
 * AI Contribution    : 100% AI generated / 0% human refactored (naive PA2 baseline, no security by design)
 * Human Modifications: None
 * Verification       : API integration tests (AssetApiIntegrationTest, TicketApiIntegrationTest) + docker compose smoke test in CI
 *                      Human review: PENDING (student to sign off: name, date, what was checked)
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.service;

import edu.bu.cs763.helpdesk.domain.Allocation;
import edu.bu.cs763.helpdesk.domain.AllocationStatus;
import edu.bu.cs763.helpdesk.domain.AssetCategory;
import edu.bu.cs763.helpdesk.repository.AllocationRepository;
import edu.bu.cs763.helpdesk.repository.AssetRepository;
import edu.bu.cs763.helpdesk.web.dto.AllocationSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AllocationService {

    private static final Logger log = LoggerFactory.getLogger(AllocationService.class);

    private final AllocationRepository allocationRepository;
    private final AssetRepository assetRepository;
    private final AssetService assetService;

    public AllocationService(AllocationRepository allocationRepository, AssetRepository assetRepository,
                             AssetService assetService) {
        this.allocationRepository = allocationRepository;
        this.assetRepository = assetRepository;
        this.assetService = assetService;
    }

    /** Active allocations, checked-out metrics and assigned software keys for a course and/or lab. */
    public AllocationSummary activeFor(String courseId, String labId) {
        List<Allocation> active = allocationRepository
                .findByStatusIn(List.of(AllocationStatus.ACTIVE, AllocationStatus.OVERDUE)).stream()
                .filter(a -> courseId == null || courseId.equals(a.getCourseId()))
                .filter(a -> labId == null || labId.equals(a.getLabId()))
                .toList();

        int checkedOut = active.stream()
                .filter(a -> a.getCategory() != null && a.getCategory().isPhysical())
                .mapToInt(Allocation::getQuantity).sum();
        int overdue = active.stream()
                .filter(a -> a.getStatus() == AllocationStatus.OVERDUE)
                .mapToInt(Allocation::getQuantity).sum();
        BigDecimal voucherCredit = active.stream()
                .map(Allocation::getVoucherAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> keys = active.stream()
                .filter(a -> a.getCategory() == AssetCategory.SOFTWARE_LICENSE)
                .map(Allocation::getIssuedToken)
                .toList();

        return new AllocationSummary(courseId, labId, active.size(), checkedOut, overdue, voucherCredit, keys, active);
    }

    /** Marks physical check-outs past their due date as OVERDUE. Runs every 15 minutes. */
    @Scheduled(fixedRateString = "${helpdesk.overdue-check-ms:900000}", initialDelay = 60000)
    @Transactional
    public int markOverdue() {
        List<Allocation> late = allocationRepository.findByStatusAndDueAtBefore(AllocationStatus.ACTIVE, Instant.now());
        for (Allocation allocation : late) {
            allocation.setStatus(AllocationStatus.OVERDUE);
            allocationRepository.save(allocation);
            assetRepository.findById(allocation.getAssetId()).ifPresent(assetService::refreshStatus);
            log.warn("Allocation overdue id={} assetId={} courseId={} labId={} dueAt={}",
                    allocation.getId(), allocation.getAssetId(), allocation.getCourseId(),
                    allocation.getLabId(), allocation.getDueAt());
        }
        if (!late.isEmpty()) {
            log.info("Overdue check: {} allocation(s) marked OVERDUE", late.size());
        }
        return late.size();
    }
}
