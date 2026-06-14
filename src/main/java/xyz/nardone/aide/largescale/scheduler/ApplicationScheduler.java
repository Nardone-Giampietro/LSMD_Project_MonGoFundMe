package xyz.nardone.aide.largescale.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.nardone.aide.largescale.service.interfaces.CampaignService;
import xyz.nardone.aide.largescale.service.interfaces.HomepageRecommendationService;
import xyz.nardone.aide.largescale.service.interfaces.OrganizationPendingRewardAgingService;
import xyz.nardone.aide.largescale.service.interfaces.OrganizationPerformanceRankingService;
import xyz.nardone.aide.largescale.service.interfaces.OutboxEventService;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.CampaignGraphService;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class ApplicationScheduler {


    private final CampaignService campaignService;
    private final HomepageRecommendationService homepageRecommendationService;
    private final OutboxEventService outboxEventService;
    private final CampaignGraphService campaignGraphService;
    private final OrganizationPendingRewardAgingService organizationPendingRewardAgingService;
    private final OrganizationPerformanceRankingService organizationPerformanceRankingService;

    @Value("${app.scheduler.outbox-cleanup-retention-days}")
    private int outboxCleanupRetentionDays;

    @Value("${app.scheduler.campaign-similarity-top-k}")
    private int campaignSimilarityTopK;

    @Value("${app.scheduler.campaign-similarity-cutoff}")
    private double campaignSimilarityCutoff;

    public ApplicationScheduler(CampaignService campaignService,
                                HomepageRecommendationService homepageRecommendationService,
                                OutboxEventService outboxEventService,
                                CampaignGraphService campaignGraphService,
                                OrganizationPendingRewardAgingService organizationPendingRewardAgingService,
                                OrganizationPerformanceRankingService organizationPerformanceRankingService) {
        this.campaignService = campaignService;
        this.homepageRecommendationService = homepageRecommendationService;
        this.outboxEventService = outboxEventService;
        this.campaignGraphService = campaignGraphService;
        this.organizationPendingRewardAgingService = organizationPendingRewardAgingService;
        this.organizationPerformanceRankingService = organizationPerformanceRankingService;
    }

    @Scheduled(cron = "${app.scheduler.campaign-state-transition-cron}")
    public void transitionOpenCampaignStates() {
        // Periodically close expired campaigns and conclude funded ones.
        campaignService.transitionOpenCampaignStates();
    }

    @Scheduled(cron = "${app.scheduler.homepage-recommendation-cron}")
    public void refreshHomepageRecommendations() {
        // Recompute the daily campaign set after campaign status transitions run.
        homepageRecommendationService.refreshHomepageRecommendations();
    }

    @Scheduled(cron = "${app.scheduler.homepage-recommendation-summary-refresh-cron}")
    public void refreshHomepageRecommendationSummaries() {
        // Refresh cached summary fields without changing the daily recommendation set.
        homepageRecommendationService.refreshHomepageRecommendationSummaries();
    }

    @Scheduled(cron = "${app.scheduler.outbox-cleanup-cron}")
    public void cleanupProcessedOutboxEvents() {
        // Remove processed outbox records after the configured retention window.
        outboxEventService.deleteProcessedEventsOlderThan(
                LocalDateTime.now().minusDays(outboxCleanupRetentionDays)
        );
    }

    @Scheduled(cron = "${app.scheduler.outbox-processing-cron}")
    public void processPendingOutboxEvents() {
        // Apply pending outbox events to external and denormalized projections.
        outboxEventService.processPendingGraphEvents();
        outboxEventService.processPendingDashboardEvents();
    }

    @Scheduled(cron = "${app.scheduler.campaign-similarity-refresh-cron}")
    public void refreshCampaignSimilarityScores() {
        // Recompute campaign similarity links used by recommendation queries.
        campaignGraphService.refreshCampaignSimilarityScores(
                campaignSimilarityTopK,
                campaignSimilarityCutoff
        );
    }

    @Scheduled(cron = "${app.scheduler.organization-pending-reward-aging-refresh-cron}")
    public void refreshOrganizationPendingRewardAgingRanking() {
        // Materialize the administrator ranking.
        organizationPendingRewardAgingService.refresh();
    }

    @Scheduled(cron = "${app.scheduler.organization-performance-ranking-refresh-cron}")
    public void refreshOrganizationPerformanceRanking() {
        // Materialize the administrator ranking.
        organizationPerformanceRankingService.refresh();
    }

}
