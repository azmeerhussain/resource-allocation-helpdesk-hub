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
 *                      Human review: Azmeer Hussain, 2026-11-16 - reviewed, seeds assets through AssetService.create(), so every seeded license key gets printed in the logs at startup
 * Confidence         : Medium
 * ======================================================================================
 */
package edu.bu.cs763.helpdesk.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.bu.cs763.helpdesk.repository.AssetRepository;
import edu.bu.cs763.helpdesk.service.AssetService;
import edu.bu.cs763.helpdesk.web.dto.AssetRequest;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Seeds the asset catalog from static mock data (mock-data/seed-assets.json) when the
 * database is empty. Enabled with helpdesk.seed=true (on in docker-compose, off in tests).
 */
@Component
@ConditionalOnProperty(name = "helpdesk.seed", havingValue = "true")
public class MockDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataLoader.class);

    private final AssetRepository assetRepository;
    private final AssetService assetService;
    private final ObjectMapper objectMapper;

    public MockDataLoader(AssetRepository assetRepository, AssetService assetService, ObjectMapper objectMapper) {
        this.assetRepository = assetRepository;
        this.assetService = assetService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (assetRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("mock-data/seed-assets.json").getInputStream()) {
            List<AssetRequest> seed = objectMapper.readValue(in, new TypeReference<>() { });
            seed.forEach(assetService::create);
            log.info("Seeded {} assets from mock data", seed.size());
        }
    }
}
