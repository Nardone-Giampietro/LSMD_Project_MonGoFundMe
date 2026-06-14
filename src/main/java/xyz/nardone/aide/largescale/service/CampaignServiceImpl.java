package xyz.nardone.aide.largescale.service;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.DTO.campaign.CampaignUpdateRequestDTO;
import xyz.nardone.aide.largescale.DTO.campaign.CreateCampaignRequestDTO;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.constant.ECampaignTag;
import xyz.nardone.aide.largescale.constant.EOrganizationStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventType;
import xyz.nardone.aide.largescale.constant.ESearchOrder;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignUpdateEntity;
import xyz.nardone.aide.largescale.exception.ApplicationErrorFactory;
import xyz.nardone.aide.largescale.mapper.CampaignMapper;
import xyz.nardone.aide.largescale.repository.CampaignRepository;
import xyz.nardone.aide.largescale.repository.neo4j.RecentExclusiveDonorCampaignResult;
import xyz.nardone.aide.largescale.service.interfaces.CampaignService;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.service.interfaces.OutboxEventService;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.CampaignGraphService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the main campaign business workflows.
 *
 * This service manages campaign creation, public campaign search, organization-only
 * campaign operations, update/milestone mutations, and campaign state transitions.
 * It also coordinates the derived dashboard and Neo4j projections through the
 * outbox so the campaign document remains the source of truth.
 */
@Service
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final MongoTemplate mongoTemplate;
    private final OutboxEventService outboxEventService;
    private final DashboardService dashboardService;
    private final CampaignMapper campaignMapper;
    private final CampaignGraphService campaignGraphService;

    public CampaignServiceImpl(CampaignRepository campaignRepository,
                               MongoTemplate mongoTemplate,
                               OutboxEventService outboxEventService,
                               DashboardService dashboardService,
                               CampaignMapper campaignMapper,
                               CampaignGraphService campaignGraphService) {
        this.campaignRepository = campaignRepository;
        this.mongoTemplate = mongoTemplate;
        this.outboxEventService = outboxEventService;
        this.dashboardService = dashboardService;
        this.campaignMapper = campaignMapper;
        this.campaignGraphService = campaignGraphService;
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public CampaignEntity createCampaign(CreateCampaignRequestDTO createCampaignRequestDTO,
                                         String organizationId,
                                         String organizationDisplayName,
                                         String organizationCity,
                                         EOrganizationStatus organizationStatus) {
        // Suspended organizations cannot open new fundraising campaigns.
        if (!EOrganizationStatus.ACTIVE.equals(organizationStatus)) {
            throw ApplicationErrorFactory.organizationSuspended(organizationId);
        }

        CampaignEntity campaign = campaignMapper.toCampaignEntity(
                createCampaignRequestDTO,
                organizationId,
                organizationDisplayName,
                organizationCity
        );

        return save(campaign);
    }

    private CampaignEntity save(CampaignEntity campaign) {
        CampaignEntity savedCampaign = campaignRepository.save(campaign);

        // Dashboard and graph projections are updated outside the campaign document.
        dashboardService.addOpenCampaign(savedCampaign);
        outboxEventService.createEvent(
                EOutboxEventType.CAMPAIGN_CREATED,
                Map.of(
                        "campaignId", savedCampaign.getId(),
                        "organizationId", savedCampaign.getOrganizationId(),
                        "title", savedCampaign.getTitle(),
                        "thumbnailImageUrl", savedCampaign.getThumbnailImageUrl(),
                        "open", ECampaignStatus.OPEN.equals(savedCampaign.getStatus())
                )
        );
        return savedCampaign;
    }

    @Override
    public Optional<CampaignEntity> findById(String id) {
        return campaignRepository.findById(id);
    }

    @Override
    public List<CampaignEntity> findClosedOrConcludedOrganizationCampaigns(String organizationId, int page, int size) {
        List<String> campaignIds = dashboardService.findClosedOrConcludedCampaignIds(organizationId, page, size);
        if (campaignIds.isEmpty()) {
            return List.of();
        }

        // Preserve the dashboard ordering after loading the campaign documents.
        Map<String, CampaignEntity> campaignsById = campaignRepository
                .findClosedOrConcludedCampaignsByIds(campaignIds, organizationId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(CampaignEntity::getId, campaign -> campaign));

        return campaignIds.stream()
                .map(campaignsById::get)
                .toList();
    }

    @Override
    public List<CampaignEntity> searchCampaigns(String text,
                                                ECampaignTag tag,
                                                ECampaignStatus status,
                                                ESearchOrder order,
                                                int page,
                                                int size) {
        long skip = (long) page * size;

        Query query = new Query()
                .with(Sort.by(ESearchOrder.OLDEST.equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt"))
                .skip(skip)
                .limit(size);

        if (text != null && !text.isBlank()) {
            // Use the Mongo text index when the caller provides free text.
            query.addCriteria(Criteria.where("$text").is(new Document("$search", text)));
        }

        if (tag != null) {
            query.addCriteria(Criteria.where("tags").is(tag));
        }

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        query.fields()
                // Project only the fields needed by campaign summary responses.
                .include("_id")
                .include("title")
                .include("thumbnailImageUrl")
                .include("tags")
                .include("status")
                .include("goalAmount")
                .include("raisedAmount")
                .include("startDate")
                .include("endDate")
                .include("organizationName")
                .include("organizationCity");

        return mongoTemplate.find(query, CampaignEntity.class);
    }

    @Override
    public List<RecentExclusiveDonorCampaignResult> findRecentExclusiveDonorCampaigns(int limit) {
        return campaignGraphService.findRecentExclusiveDonorCampaigns(limit);
    }

    @Override
    public void addUpdate(String campaignId, String organizationId, CampaignUpdateRequestDTO updateDTO) {
        validateOpenCampaignOwnership(campaignId, organizationId);
        LocalDateTime now = LocalDateTime.now();

        // The repository enforces the maximum number of embedded updates.
        long updatedCampaigns = campaignRepository.addUpdate(
                campaignId,
                organizationId,
                new CampaignUpdateEntity(
                        UUID.randomUUID().toString(),
                        now,
                        updateDTO.getTitle(),
                        updateDTO.getDescription()
                ),
                now
        );

        if (updatedCampaigns == 0) {
            throw ApplicationErrorFactory.campaignUpdateLimitReached(campaignId);
        }
    }

    @Override
    public void deleteUpdate(String campaignId, String organizationId, String updateId) {
        validateOpenCampaignOwnership(campaignId, organizationId);

        long updatedCampaigns = campaignRepository.deleteUpdate(campaignId, organizationId, updateId, LocalDateTime.now());
        if (updatedCampaigns == 0) {
            throw ApplicationErrorFactory.campaignUpdateNotFound(updateId);
        }
    }

    @Override
    public void verifyMilestone(String campaignId, String organizationId, String milestoneId) {
        validateOpenCampaignOwnership(campaignId, organizationId);

        long updatedCampaigns = campaignRepository.verifyMilestone(campaignId, organizationId, milestoneId, LocalDateTime.now());
        if (updatedCampaigns == 0) {
            throw ApplicationErrorFactory.milestoneNotFound(milestoneId);
        }
    }

    @Override
    public CampaignEntity findOwnedByOrganization(String campaignId, String organizationId) {
        CampaignEntity campaign = findById(campaignId)
                .orElseThrow(() -> ApplicationErrorFactory.campaignNotFound(campaignId));

        if (!campaign.getOrganizationId().equals(organizationId)) {
            throw ApplicationErrorFactory.campaignNotFound(campaignId);
        }

        return campaign;
    }

    private void validateOpenCampaignOwnership(String campaignId, String organizationId) {
        if (!campaignRepository.existsByIdAndOrganizationIdAndStatus(campaignId, organizationId, ECampaignStatus.OPEN)) {
            throw ApplicationErrorFactory.campaignNotFound(campaignId);
        }
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public void transitionOpenCampaignStates() {
        LocalDateTime now = LocalDateTime.now();
        List<CampaignEntity> transitionedCampaigns = campaignRepository.findOpenCampaignsToTransition(now);

        if (transitionedCampaigns.isEmpty()) {
            return;
        }

        // Funded campaigns conclude; expired unfunded campaigns close.
        campaignRepository.transitionOpenCampaignStates(now);

        for (CampaignEntity campaign : transitionedCampaigns) {
            // Remove closed campaigns from dashboards and mark them closed in Neo4j later.
            dashboardService.archiveOpenCampaign(campaign.getOrganizationId(), campaign.getId());
            outboxEventService.createEvent(
                    EOutboxEventType.CAMPAIGN_STATUS_CHANGED,
                    Map.of(
                            "campaignId", campaign.getId(),
                            "open", false
                    )
            );
        }
    }

}
