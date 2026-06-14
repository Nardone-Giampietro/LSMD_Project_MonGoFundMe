package xyz.nardone.aide.largescale.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.constant.EDonationStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventType;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.entity.OutboxEventEntity;
import xyz.nardone.aide.largescale.repository.OutboxEventRepository;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.service.interfaces.OutboxEventService;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.CampaignGraphService;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.DonorGraphService;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.OrganizationGraphService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the outbox processing layer for derived projections.
 *
 * Business transactions write pending outbox events inside MongoDB. This service
 * later dispatches those events to dashboard projections and Neo4j graph writes,
 * marking an event as processed only after the corresponding side effect
 * succeeds so failed work can be retried by the scheduler.
 */
@Service
public class OutboxEventServiceImpl implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final DashboardService dashboardService;
    private final DonorGraphService donorGraphService;
    private final OrganizationGraphService organizationGraphService;
    private final CampaignGraphService campaignGraphService;

    public OutboxEventServiceImpl(OutboxEventRepository outboxEventRepository,
                                  DashboardService dashboardService,
                                  DonorGraphService donorGraphService,
                                  OrganizationGraphService organizationGraphService,
                                  CampaignGraphService campaignGraphService) {
        this.outboxEventRepository = outboxEventRepository;
        this.dashboardService = dashboardService;
        this.donorGraphService = donorGraphService;
        this.organizationGraphService = organizationGraphService;
        this.campaignGraphService = campaignGraphService;
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public void createEvent(EOutboxEventType eventType, Map<String, Object> payload) {
        // Store side effects as pending events so they can be retried asynchronously.
        OutboxEventEntity event = new OutboxEventEntity(
                null,
                eventType,
                payload,
                EOutboxEventStatus.PENDING,
                LocalDateTime.now(),
                null
        );

        outboxEventRepository.save(event);
    }

    private OutboxEventEntity markProcessed(OutboxEventEntity event) {
        // The processed timestamp is used by the cleanup scheduler.
        event.setStatus(EOutboxEventStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        return outboxEventRepository.save(event);
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public long deleteProcessedEventsOlderThan(LocalDateTime processedBefore) {
        // Keep only recent processed events; pending failures are never removed here.
        return outboxEventRepository.deleteByStatusAndProcessedAtBefore(
                EOutboxEventStatus.PROCESSED,
                processedBefore
        );
    }

    @Override
    public void processPendingGraphEvents() {
        // Graph events are processed in creation order to keep Neo4j state consistent.
        List<OutboxEventEntity> pendingEvents = outboxEventRepository
                .findByStatusAndEventTypeInOrderByCreatedAtAsc(
                        EOutboxEventStatus.PENDING,
                        List.of(
                                EOutboxEventType.DONOR_CREATED,
                                EOutboxEventType.ORGANIZATION_CREATED,
                                EOutboxEventType.ORGANIZATION_SUSPENDED,
                                EOutboxEventType.CAMPAIGN_CREATED,
                                EOutboxEventType.DONATION_CREATED_GRAPH,
                                EOutboxEventType.CAMPAIGN_STATUS_CHANGED
                        )
                );

        for (OutboxEventEntity event : pendingEvents) {
            try {
                // Dispatch the event to the graph service that owns it.
                processGraphEvent(event);
            } catch (RuntimeException ignored) {
                // Keep failed events pending so the scheduler can retry them later.
                continue;
            }

            // Mark the event processed only after the graph write succeeds.
            markProcessed(event);
        }
    }

    @Override
    public void processPendingDashboardEvents() {
        // Read pending dashboard events in creation order.
        List<OutboxEventEntity> pendingEvents = outboxEventRepository
                .findByStatusAndEventTypeInOrderByCreatedAtAsc(
                        EOutboxEventStatus.PENDING,
                        List.of(
                                EOutboxEventType.DONATION_CREATED_DASHBOARD,
                                EOutboxEventType.DONATION_CONCLUDED_DASHBOARD
                        )
                );
        if (pendingEvents.isEmpty()) {
            return;
        }

        // Group payloads into snapshots for bulk dashboard updates.
        List<DonationEntity> createdDonationSnapshots = new ArrayList<>();
        List<DonationEntity> statusSnapshots = new ArrayList<>();
        Map<String, CampaignEntity> campaignSnapshotsById = new LinkedHashMap<>();
        List<String> processedEventIds = new ArrayList<>();

        for (OutboxEventEntity event : pendingEvents) {
            if (EOutboxEventType.DONATION_CREATED_DASHBOARD.equals(event.getEventType())) {
                // New donations update donor summaries and campaign raised amounts.
                DonationEntity donation = toCreatedDonationSnapshot(event.getPayload());
                createdDonationSnapshots.add(donation);
                putHighestCampaignRaisedAmount(campaignSnapshotsById, event.getPayload());
                processedEventIds.add(event.getId());
            }

            if (EOutboxEventType.DONATION_CONCLUDED_DASHBOARD.equals(event.getEventType())) {
                // Concluded reward donations only need a dashboard status update.
                statusSnapshots.add(toDonationStatusSnapshot(event.getPayload()));
                processedEventIds.add(event.getId());
            }
        }

        // Apply the dashboard projection updates in bulk.
        dashboardService.syncDonorDonationSummaries(createdDonationSnapshots);
        dashboardService.syncDonorDonationStatuses(statusSnapshots);
        dashboardService.syncOpenCampaignRaisedAmounts(new ArrayList<>(campaignSnapshotsById.values()));

        if (!processedEventIds.isEmpty()) {
            // Mark events processed only after all dashboard writes succeed.
            outboxEventRepository.markProcessedByIds(processedEventIds, LocalDateTime.now());
        }
    }

    private void processGraphEvent(OutboxEventEntity event) {
        // Dispatch each event type to the graph service that owns the projection.
        switch (event.getEventType()) {
            case DONOR_CREATED -> donorGraphService.createDonor((String) event.getPayload().get("donorId"));
            case ORGANIZATION_CREATED -> organizationGraphService.createOrganization(
                    (String) event.getPayload().get("organizationId")
            );
            case ORGANIZATION_SUSPENDED -> organizationGraphService.deleteOrganizationSubgraph(
                    (String) event.getPayload().get("organizationId")
            );
            case CAMPAIGN_CREATED -> campaignGraphService.createCampaign(
                    (String) event.getPayload().get("campaignId"),
                    (String) event.getPayload().get("organizationId"),
                    (String) event.getPayload().get("title"),
                    (String) event.getPayload().get("thumbnailImageUrl"),
                    (Boolean) event.getPayload().get("open")
            );
            case DONATION_CREATED_GRAPH -> campaignGraphService.addDonation(
                    (String) event.getPayload().get("campaignId"),
                    (String) event.getPayload().get("donorId"),
                    LocalDateTime.parse((String) event.getPayload().get("donatedAt"))
            );
            case CAMPAIGN_STATUS_CHANGED -> campaignGraphService.updateCampaignOpen(
                    (String) event.getPayload().get("campaignId"),
                    (Boolean) event.getPayload().get("open")
            );
            default -> {
            }
        }
    }

    private DonationEntity toCreatedDonationSnapshot(Map<String, Object> payload) {
        DonationEntity donation = new DonationEntity();
        donation.setId(payload.get("donationId").toString());
        donation.setDonorId(payload.get("donorId").toString());
        donation.setCampaignId(payload.get("campaignId").toString());
        donation.setCampaignTitle(payload.get("campaignTitle").toString());
        donation.setAmount(new BigDecimal(payload.get("amount").toString()));
        donation.setDonatedAt(LocalDateTime.parse(payload.get("donatedAt").toString()));
        donation.setStatus(EDonationStatus.valueOf(payload.get("status").toString()));
        return donation;
    }

    private DonationEntity toDonationStatusSnapshot(Map<String, Object> payload) {
        DonationEntity donation = new DonationEntity();
        donation.setId(payload.get("donationId").toString());
        donation.setDonorId(payload.get("donorId").toString());
        donation.setStatus(EDonationStatus.CONCLUDED);
        return donation;
    }

    private void putHighestCampaignRaisedAmount(Map<String, CampaignEntity> campaignSnapshotsById,
                                                Map<String, Object> payload) {
        String campaignId = payload.get("campaignId").toString();
        BigDecimal raisedAmount = new BigDecimal(payload.get("campaignRaisedAmount").toString());
        CampaignEntity existingCampaign = campaignSnapshotsById.get(campaignId);

        // Keep the highest amount when multiple donation events touch the same campaign.
        if (existingCampaign != null && existingCampaign.getRaisedAmount().compareTo(raisedAmount) >= 0) {
            return;
        }

        CampaignEntity campaign = new CampaignEntity();
        campaign.setId(campaignId);
        campaign.setOrganizationId(payload.get("organizationId").toString());
        campaign.setRaisedAmount(raisedAmount);
        campaignSnapshotsById.put(campaignId, campaign);
    }
}
