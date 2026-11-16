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
import edu.bu.cs763.helpdesk.domain.Asset;
import edu.bu.cs763.helpdesk.domain.AssetCategory;
import edu.bu.cs763.helpdesk.domain.AssetStatus;
import edu.bu.cs763.helpdesk.repository.AllocationRepository;
import edu.bu.cs763.helpdesk.repository.AssetRepository;
import edu.bu.cs763.helpdesk.web.dto.AssetRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);
    private static final List<AllocationStatus> OUTSTANDING = List.of(AllocationStatus.ACTIVE, AllocationStatus.OVERDUE);

    private final AssetRepository assetRepository;
    private final AllocationRepository allocationRepository;

    public AssetService(AssetRepository assetRepository, AllocationRepository allocationRepository) {
        this.assetRepository = assetRepository;
        this.allocationRepository = allocationRepository;
    }

    @Transactional
    public Asset create(AssetRequest request) {
        Asset asset = new Asset();
        apply(asset, request);
        if (asset.getTotalStock() == null) {
            asset.setTotalStock(1);
        }
        if (asset.getAvailableStock() == null) {
            asset.setAvailableStock(asset.getTotalStock());
        }
        if (asset.getMaxCheckoutDays() == null && asset.getCategory() != null && asset.getCategory().isPhysical()) {
            asset.setMaxCheckoutDays(14);
        }
        asset.setCreatedAt(Instant.now());
        asset.setUpdatedAt(asset.getCreatedAt());
        if (asset.getStatus() == null) {
            asset.setStatus(AssetStatus.AVAILABLE);
        }
        Asset saved = assetRepository.save(asset);
        log.info("Asset created id={} name={} category={} stock={} balance={} licenseKey={}",
                saved.getId(), saved.getName(), saved.getCategory(), saved.getTotalStock(),
                saved.getVoucherBalance(), saved.getLicenseKey());
        return saved;
    }

    public List<Asset> search(AssetStatus status, AssetCategory category, String departmentProgram,
                              String courseId, String labId, String q) {
        Specification<Asset> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (departmentProgram != null) {
                predicates.add(cb.equal(root.get("departmentProgram"), departmentProgram));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            if (labId != null) {
                predicates.add(cb.equal(root.get("labId"), labId));
            }
            if (q != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return assetRepository.findAll(spec, Sort.by("createdAt"));
    }

    public Asset get(UUID id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + id));
    }

    @Transactional
    public Asset update(UUID id, AssetRequest request) {
        Asset asset = get(id);
        apply(asset, request);
        asset.setUpdatedAt(Instant.now());
        log.info("Asset updated id={}", id);
        return assetRepository.save(asset);
    }

    @Transactional
    public void delete(UUID id) {
        Asset asset = get(id);
        assetRepository.delete(asset);
        log.info("Asset deleted id={} name={}", id, asset.getName());
    }

    /**
     * Returns a checked-out physical device: closes the allocation, gives the units back
     * to the pool and records the return timestamp.
     */
    @Transactional
    public Allocation returnAsset(UUID assetId, UUID allocationId) {
        Asset asset = get(assetId);
        if (asset.getCategory() == null || !asset.getCategory().isPhysical()) {
            throw new ConflictException("Only physical assets can be returned: " + assetId);
        }
        Allocation allocation;
        if (allocationId != null) {
            allocation = allocationRepository.findById(allocationId)
                    .orElseThrow(() -> new NotFoundException("Allocation not found: " + allocationId));
            if (!allocation.getAssetId().equals(assetId)) {
                throw new ConflictException("Allocation " + allocationId + " does not belong to asset " + assetId);
            }
        } else {
            allocation = allocationRepository
                    .findByAssetIdAndStatusInOrderByCheckedOutAtAsc(assetId, OUTSTANDING)
                    .stream().findFirst()
                    .orElseThrow(() -> new ConflictException("Asset has no checked-out units: " + assetId));
        }
        if (allocation.getStatus() == AllocationStatus.RETURNED) {
            throw new ConflictException("Allocation already returned: " + allocation.getId());
        }

        allocation.setStatus(AllocationStatus.RETURNED);
        allocation.setReturnedAt(Instant.now());
        allocationRepository.save(allocation);

        asset.setAvailableStock(asset.getAvailableStock() + allocation.getQuantity());
        asset.setUpdatedAt(Instant.now());
        assetRepository.save(asset);
        refreshStatus(asset);

        log.info("Asset returned assetId={} allocationId={} quantity={} durationHours={}",
                assetId, allocation.getId(), allocation.getQuantity(), allocation.getCheckoutDurationHours());
        return allocation;
    }

    /** Recalculates the asset status flag from stock and outstanding allocations. */
    @Transactional
    public void refreshStatus(Asset asset) {
        AssetStatus newStatus;
        if (allocationRepository.existsByAssetIdAndStatus(asset.getId(), AllocationStatus.OVERDUE)) {
            newStatus = AssetStatus.OVERDUE;
        } else if (asset.getCategory() == AssetCategory.CLOUD_VOUCHER) {
            boolean hasBalance = asset.getVoucherBalance() != null && asset.getVoucherBalance().signum() > 0;
            newStatus = hasBalance ? AssetStatus.AVAILABLE : AssetStatus.CHECKED_OUT;
        } else if (asset.getAvailableStock() != null && asset.getAvailableStock() <= 0) {
            newStatus = AssetStatus.CHECKED_OUT;
        } else {
            newStatus = AssetStatus.AVAILABLE;
        }
        asset.setStatus(newStatus);
        assetRepository.save(asset);
    }

    private void apply(Asset asset, AssetRequest r) {
        if (r.name() != null) {
            asset.setName(r.name());
        }
        if (r.category() != null) {
            asset.setCategory(r.category());
        }
        if (r.totalStock() != null) {
            asset.setTotalStock(r.totalStock());
        }
        if (r.availableStock() != null) {
            asset.setAvailableStock(r.availableStock());
        }
        if (r.status() != null) {
            asset.setStatus(r.status());
        }
        if (r.departmentProgram() != null) {
            asset.setDepartmentProgram(r.departmentProgram());
        }
        if (r.courseId() != null) {
            asset.setCourseId(r.courseId());
        }
        if (r.labId() != null) {
            asset.setLabId(r.labId());
        }
        if (r.voucherBalance() != null) {
            asset.setVoucherBalance(r.voucherBalance());
        }
        if (r.licenseKey() != null) {
            asset.setLicenseKey(r.licenseKey());
        }
        if (r.maxCheckoutDays() != null) {
            asset.setMaxCheckoutDays(r.maxCheckoutDays());
        }
    }
}
