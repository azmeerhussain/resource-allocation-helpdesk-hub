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
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed allocate() and transition(), found race condition + shared license key, the key is printed in the logs, rejecting an approved ticket leaves assets stuck as RESERVED, and a negative quantity or voucher amount adds stock/balance instead of taking it away
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.service;

import edu.bu.cs763.helpdesk.domain.Allocation;
import edu.bu.cs763.helpdesk.domain.AllocationStatus;
import edu.bu.cs763.helpdesk.domain.Asset;
import edu.bu.cs763.helpdesk.domain.AssetCategory;
import edu.bu.cs763.helpdesk.domain.AssetStatus;
import edu.bu.cs763.helpdesk.domain.Ticket;
import edu.bu.cs763.helpdesk.domain.TicketEvent;
import edu.bu.cs763.helpdesk.domain.TicketItem;
import edu.bu.cs763.helpdesk.domain.TicketStatus;
import edu.bu.cs763.helpdesk.repository.AllocationRepository;
import edu.bu.cs763.helpdesk.repository.AssetRepository;
import edu.bu.cs763.helpdesk.repository.TicketEventRepository;
import edu.bu.cs763.helpdesk.repository.TicketRepository;
import edu.bu.cs763.helpdesk.web.dto.TicketDetails;
import edu.bu.cs763.helpdesk.web.dto.TicketItemRequest;
import edu.bu.cs763.helpdesk.web.dto.TicketRequest;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    /** Allowed review transitions through PUT. ALLOCATED is only reachable through /allocate. */
    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
            TicketStatus.PENDING, Set.of(TicketStatus.APPROVED, TicketStatus.REJECTED),
            TicketStatus.APPROVED, Set.of(TicketStatus.REJECTED),
            TicketStatus.REJECTED, Set.of(TicketStatus.CLOSED),
            TicketStatus.ALLOCATED, Set.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED, Set.of());

    private final TicketRepository ticketRepository;
    private final TicketEventRepository eventRepository;
    private final AssetRepository assetRepository;
    private final AllocationRepository allocationRepository;
    private final AssetService assetService;

    public TicketService(TicketRepository ticketRepository, TicketEventRepository eventRepository,
                         AssetRepository assetRepository, AllocationRepository allocationRepository,
                         AssetService assetService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.assetRepository = assetRepository;
        this.allocationRepository = allocationRepository;
        this.assetService = assetService;
    }

    @Transactional
    public Ticket create(TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setRequesterName(request.requesterName());
        ticket.setRequesterRole(request.requesterRole());
        ticket.setCourseId(request.courseId());
        ticket.setLabId(request.labId());
        ticket.setProjectName(request.projectName());
        ticket.setJustification(request.justification());
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(ticket.getCreatedAt());
        addItems(ticket, request.items());

        Ticket saved = ticketRepository.save(ticket);
        recordEvent(saved.getId(), null, TicketStatus.PENDING, saved.getRequesterName(), "Ticket submitted");
        log.info("Ticket submitted id={} requester={} role={} courseId={} labId={} items={}",
                saved.getId(), saved.getRequesterName(), saved.getRequesterRole(),
                saved.getCourseId(), saved.getLabId(), saved.getItems().size());
        return saved;
    }

    public List<Ticket> search(TicketStatus status, String courseId, String labId, String projectName,
                               UUID assetId) {
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            if (labId != null) {
                predicates.add(cb.equal(root.get("labId"), labId));
            }
            if (projectName != null) {
                predicates.add(cb.equal(root.get("projectName"), projectName));
            }
            if (assetId != null) {
                query.distinct(true);
                predicates.add(cb.equal(root.join("items").get("assetId"), assetId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ticketRepository.findAll(spec, Sort.by("createdAt"));
    }

    public Ticket get(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
    }

    public TicketDetails details(UUID id) {
        Ticket ticket = get(id);
        List<TicketEvent> history = eventRepository.findByTicketIdOrderByOccurredAtAsc(id);
        List<Allocation> allocations = allocationRepository.findByTicketId(id);

        int totalQuantity = ticket.getItems().stream()
                .map(TicketItem::getQuantity).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        BigDecimal totalVoucher = ticket.getItems().stream()
                .map(TicketItem::getVoucherAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant end = ticket.getStatus() == TicketStatus.CLOSED ? ticket.getUpdatedAt() : Instant.now();
        long hoursOpen = Duration.between(ticket.getCreatedAt(), end).toHours();

        TicketDetails.Metrics metrics = new TicketDetails.Metrics(
                ticket.getItems().size(), totalQuantity, totalVoucher, hoursOpen, history.size());
        return new TicketDetails(ticket, history, allocations, metrics);
    }

    /**
     * PUT handler. If the body carries a status, this is the review / approval state transition
     * (IT director sign-off). Otherwise the ticket contents are updated (only while PENDING).
     */
    @Transactional
    public Ticket update(UUID id, TicketRequest request) {
        Ticket ticket = get(id);
        if (request.status() != null && request.status() != ticket.getStatus()) {
            return transition(ticket, request);
        }
        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new ConflictException("Only PENDING tickets can be edited; ticket is " + ticket.getStatus());
        }
        if (request.requesterName() != null) {
            ticket.setRequesterName(request.requesterName());
        }
        if (request.requesterRole() != null) {
            ticket.setRequesterRole(request.requesterRole());
        }
        if (request.courseId() != null) {
            ticket.setCourseId(request.courseId());
        }
        if (request.labId() != null) {
            ticket.setLabId(request.labId());
        }
        if (request.projectName() != null) {
            ticket.setProjectName(request.projectName());
        }
        if (request.justification() != null) {
            ticket.setJustification(request.justification());
        }
        if (request.items() != null) {
            ticket.getItems().clear();
            addItems(ticket, request.items());
        }
        ticket.setUpdatedAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);
        recordEvent(id, TicketStatus.PENDING, TicketStatus.PENDING, ticket.getRequesterName(), "Ticket contents updated");
        log.info("Ticket updated id={}", id);
        return saved;
    }

    private Ticket transition(Ticket ticket, TicketRequest request) {
        TicketStatus from = ticket.getStatus();
        TicketStatus to = request.status();
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new ConflictException("Invalid ticket transition " + from + " -> " + to
                    + (to == TicketStatus.ALLOCATED ? " (use POST /allocate)" : ""));
        }
        ticket.setStatus(to);
        ticket.setReviewedBy(request.reviewedBy());
        ticket.setReviewNote(request.reviewNote());
        ticket.setReviewedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);

        if (to == TicketStatus.APPROVED) {
            // Approval only flags the requested physical assets as RESERVED; inventory is untouched
            // until the separate /allocate command runs.
            for (TicketItem item : ticket.getItems()) {
                assetRepository.findById(item.getAssetId()).ifPresent(asset -> {
                    if (asset.getCategory() != null && asset.getCategory().isPhysical()
                            && asset.getStatus() == AssetStatus.AVAILABLE) {
                        asset.setStatus(AssetStatus.RESERVED);
                        assetRepository.save(asset);
                    }
                });
            }
        }
        recordEvent(ticket.getId(), from, to, request.reviewedBy(), request.reviewNote());
        log.info("Ticket transition id={} {} -> {} by={}", ticket.getId(), from, to, request.reviewedBy());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Ticket ticket = get(id);
        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new ConflictException("Only PENDING tickets can be deleted; ticket is " + ticket.getStatus());
        }
        eventRepository.deleteByTicketId(id);
        ticketRepository.delete(ticket);
        log.info("Ticket deleted id={}", id);
    }

    /**
     * Allocation execution command: processes an APPROVED ticket, deducts inventory atomically
     * (all items or none) and issues license keys / voucher codes / hardware assignment tokens.
     */
    @Transactional
    public List<Allocation> allocate(UUID id, String allocatedBy) {
        Ticket ticket = get(id);
        if (ticket.getStatus() != TicketStatus.APPROVED) {
            throw new ConflictException("Only APPROVED tickets can be allocated; ticket is " + ticket.getStatus());
        }
        List<Allocation> created = new ArrayList<>();
        Instant now = Instant.now();

        for (TicketItem item : ticket.getItems()) {
            Asset asset = assetRepository.findById(item.getAssetId())
                    .orElseThrow(() -> new NotFoundException("Asset not found: " + item.getAssetId()));
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();

            Allocation allocation = new Allocation();
            allocation.setTicketId(ticket.getId());
            allocation.setAssetId(asset.getId());
            allocation.setAssetName(asset.getName());
            allocation.setCategory(asset.getCategory());
            allocation.setCourseId(ticket.getCourseId());
            allocation.setLabId(ticket.getLabId());
            allocation.setQuantity(quantity);
            allocation.setStatus(AllocationStatus.ACTIVE);
            allocation.setCheckedOutAt(now);

            if (asset.getCategory() == AssetCategory.CLOUD_VOUCHER) {
                BigDecimal amount = item.getVoucherAmount() == null ? BigDecimal.ZERO : item.getVoucherAmount();
                BigDecimal balance = asset.getVoucherBalance() == null ? BigDecimal.ZERO : asset.getVoucherBalance();
                BigDecimal remaining = balance.subtract(amount);
                if (remaining.signum() < 0) {
                    throw new ConflictException("Insufficient voucher balance on asset " + asset.getId()
                            + ": balance " + balance + ", requested " + amount);
                }
                asset.setVoucherBalance(remaining);
                allocation.setVoucherAmount(amount);
                allocation.setIssuedToken("VCH-" + randomToken());
            } else {
                int available = asset.getAvailableStock() == null ? 0 : asset.getAvailableStock();
                if (available < quantity) {
                    throw new ConflictException("Insufficient stock on asset " + asset.getId()
                            + ": available " + available + ", requested " + quantity);
                }
                asset.setAvailableStock(available - quantity);
                if (asset.getCategory() == AssetCategory.SOFTWARE_LICENSE) {
                    allocation.setIssuedToken(asset.getLicenseKey());
                } else {
                    allocation.setIssuedToken("HW-" + randomToken());
                    int days = asset.getMaxCheckoutDays() == null ? 14 : asset.getMaxCheckoutDays();
                    allocation.setDueAt(now.plus(days, ChronoUnit.DAYS));
                }
            }
            asset.setUpdatedAt(now);
            assetRepository.save(asset);
            Allocation saved = allocationRepository.save(allocation);
            assetService.refreshStatus(asset);
            created.add(saved);
            log.info("Allocated ticketId={} assetId={} category={} quantity={} voucherAmount={} token={} courseId={} labId={}",
                    ticket.getId(), asset.getId(), asset.getCategory(), quantity, saved.getVoucherAmount(),
                    saved.getIssuedToken(), ticket.getCourseId(), ticket.getLabId());
        }

        ticket.setStatus(TicketStatus.ALLOCATED);
        ticket.setAllocatedBy(allocatedBy);
        ticket.setAllocatedAt(now);
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);
        recordEvent(ticket.getId(), TicketStatus.APPROVED, TicketStatus.ALLOCATED, allocatedBy,
                created.size() + " allocation(s) executed");
        log.info("Ticket allocated id={} allocations={} by={}", ticket.getId(), created.size(), allocatedBy);
        return created;
    }

    private void addItems(Ticket ticket, List<TicketItemRequest> items) {
        if (items == null) {
            return;
        }
        for (TicketItemRequest r : items) {
            TicketItem item = new TicketItem();
            item.setAssetId(r.assetId());
            item.setQuantity(r.quantity() == null ? 1 : r.quantity());
            item.setVoucherAmount(r.voucherAmount());
            ticket.addItem(item);
        }
    }

    private void recordEvent(UUID ticketId, TicketStatus from, TicketStatus to, String actor, String note) {
        TicketEvent event = new TicketEvent();
        event.setTicketId(ticketId);
        event.setFromStatus(from == null ? null : from.name());
        event.setToStatus(to.name());
        event.setActor(actor);
        event.setNote(note);
        event.setOccurredAt(Instant.now());
        eventRepository.save(event);
    }

    private static String randomToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
