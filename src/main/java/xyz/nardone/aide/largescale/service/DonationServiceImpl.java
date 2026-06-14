package xyz.nardone.aide.largescale.service;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.DTO.donation.CreateDonationRequestDTO;
import xyz.nardone.aide.largescale.constant.EDonationStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventType;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignAvailableRewardEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignLatestDonationEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignPendingRewardEntity;
import xyz.nardone.aide.largescale.exception.ApplicationErrorFactory;
import xyz.nardone.aide.largescale.mapper.DonationSnapshotMapper;
import xyz.nardone.aide.largescale.repository.CampaignRepository;
import xyz.nardone.aide.largescale.repository.DonationRepository;
import xyz.nardone.aide.largescale.service.interfaces.DonationService;
import xyz.nardone.aide.largescale.service.interfaces.OutboxEventService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements donation creation, lookup, and conclusion workflows.
 *
 * Writes donation document and update the related
 * campaign counters, latest donations, pending rewards, and concluded donation
 * ids. The service also emits outbox events so dashboards and Neo4j projections
 * can be synchronized asynchronously after the MongoDB transaction commits.
 */
@Service
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final MongoTemplate mongoTemplate;
    private final DonationSnapshotMapper donationSnapshotMapper;
    private final OutboxEventService outboxEventService;

    public DonationServiceImpl(DonationRepository donationRepository,
                               CampaignRepository campaignRepository,
                               MongoTemplate mongoTemplate,
                               DonationSnapshotMapper donationSnapshotMapper,
                               OutboxEventService outboxEventService) {
        this.donationRepository = donationRepository;
        this.campaignRepository = campaignRepository;
        this.mongoTemplate = mongoTemplate;
        this.donationSnapshotMapper = donationSnapshotMapper;
        this.outboxEventService = outboxEventService;
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public DonationEntity createDonation(CreateDonationRequestDTO requestDTO, UserDetailsImpl donor) {
        // Load the campaign from the donation-specific read model before creating snapshots.
        CampaignEntity campaign = campaignRepository.findCampaignForDonationCreationById(requestDTO.getCampaignId())
                .orElseThrow(() -> ApplicationErrorFactory.campaignNotFound(requestDTO.getCampaignId()));
        LocalDateTime now = LocalDateTime.now();

        String rewardId = requestDTO.getRewardId();
        CampaignAvailableRewardEntity selectedReward = null;
        if (rewardId != null && !rewardId.isBlank()) {
            // Find the specifics of the reward that was selected
            selectedReward = campaign.getAvailableRewards().stream()
                    .filter(reward -> rewardId.equals(reward.getRewardId()))
                    .findFirst()
                    .orElseThrow(() -> ApplicationErrorFactory.rewardNotFound(rewardId));
        }

        // Store the donation with snapshots of donor, campaign, and reward data.
        DonationEntity savedDonation = donationRepository.save(
                donationSnapshotMapper.toDonationEntity(donor, campaign, selectedReward, requestDTO.getAmount(), now)
        );
        BigDecimal campaignRaisedAmount = campaign.getRaisedAmount().add(savedDonation.getAmount());

        // Keep campaign aggregates and async projections updated with the new donation.
        updateCampaign(campaign, savedDonation, selectedReward);
        outboxEventService.createEvent(
                EOutboxEventType.DONATION_CREATED_GRAPH,
                Map.of(
                        "campaignId", savedDonation.getCampaignId(),
                        "donorId", savedDonation.getDonorId(),
                        "donatedAt", savedDonation.getDonatedAt().toString()
                )
        );
        outboxEventService.createEvent(
                EOutboxEventType.DONATION_CREATED_DASHBOARD,
                Map.of(
                        "donationId", savedDonation.getId(),
                        "donorId", savedDonation.getDonorId(),
                        "campaignId", savedDonation.getCampaignId(),
                        "campaignTitle", savedDonation.getCampaignTitle(),
                        "organizationId", savedDonation.getOrganizationId(),
                        "amount", savedDonation.getAmount().toPlainString(),
                        "donatedAt", savedDonation.getDonatedAt().toString(),
                        "status", savedDonation.getStatus().name(),
                        "campaignRaisedAmount", campaignRaisedAmount.toPlainString()
                )
        );

        return savedDonation;
    }

    @Override
    public DonationEntity findDonorDonation(String donationId, String donorId) {
        return donationRepository.findByIdAndDonorId(donationId, donorId)
                .orElseThrow(() -> ApplicationErrorFactory.donationNotFound(donationId));
    }

    @Override
    public DonationEntity findOrganizationDonation(String donationId, String organizationId) {
        return donationRepository.findByIdAndOrganizationId(donationId, organizationId)
                .orElseThrow(() -> ApplicationErrorFactory.donationNotFound(donationId));
    }

    @Override
    public List<CampaignPendingRewardEntity> findOrganizationCampaignPendingRewards(String campaignId,
                                                                                   String organizationId,
                                                                                   int page,
                                                                                   int size) {
        CampaignEntity campaign = campaignRepository.findDonationReadModelByIdAndOrganizationId(campaignId, organizationId)
                .orElseThrow(() -> ApplicationErrorFactory.campaignNotFound(campaignId));
        return campaign.getPendingRewards().stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    @Override
    public List<DonationEntity> findOrganizationCampaignConcludedDonations(String campaignId,
                                                                           String organizationId,
                                                                           int page,
                                                                           int size) {
        CampaignEntity campaign = campaignRepository.findDonationReadModelByIdAndOrganizationId(campaignId, organizationId)
                .orElseThrow(() -> ApplicationErrorFactory.campaignNotFound(campaignId));
        List<String> pagedDonationIds = campaign.getConcludedDonationIds().stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
        if (pagedDonationIds.isEmpty()) {
            return List.of();
        }

        // Fetch by id, then return in the same order stored on the campaign document.
        Map<String, DonationEntity> donationsById = donationRepository
                .findConcludedDonationsByIds(pagedDonationIds, organizationId, campaignId)
                .stream()
                .collect(Collectors.toMap(DonationEntity::getId, donation -> donation));

        return pagedDonationIds.stream()
                .map(donationsById::get)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public void concludeDonation(String donationId, String donorId) {
        // Atomically claim only pending reward donations owned by this donor.
        Query query = Query.query(Criteria.where("_id").is(donationId)
                .and("donorId").is(donorId)
                .and("status").is(EDonationStatus.PENDING)
                .and("reward").ne(null));
        query.fields().include("campaignId");

        DonationEntity donation = mongoTemplate.findAndModify(
                query,
                Update.update("status", EDonationStatus.CONCLUDED),
                FindAndModifyOptions.options().returnNew(false),
                DonationEntity.class
        );

        if (donation == null) {
            return;
        }

        // Move the donation id from the embedded pending queue to the concluded list.
        long updatedCampaigns = campaignRepository.movePendingRewardToConcludedDonations(
                donation.getCampaignId(),
                donationId,
                LocalDateTime.now()
        );
        if (updatedCampaigns == 0) {
            throw ApplicationErrorFactory.campaignNotFound(donation.getCampaignId());
        }

        outboxEventService.createEvent(
                EOutboxEventType.DONATION_CONCLUDED_DASHBOARD,
                Map.of(
                        "donationId", donationId,
                        "donorId", donorId
                )
        );
    }

    private void updateCampaign(CampaignEntity campaign,
                                DonationEntity donation,
                                CampaignAvailableRewardEntity selectedReward) {
        // Update the public campaign counters and keep only the latest ten donations.
        Update update = new Update()
                .push("latestDonations")
                .atPosition(0)
                .slice(10)
                .each(new CampaignLatestDonationEntity(
                        donation.getDonorSnapshot().getFullName(),
                        donation.getAmount(),
                        donation.getDonatedAt()
                ))
                .inc("donationsCount", 1)
                .inc("raisedAmount", donation.getAmount())
                .set("updatedAt", LocalDateTime.now());

        if (selectedReward != null) {
            // Reward donations wait in a queue until the donor concludes them.
            update.push("pendingRewards")
                    .atPosition(0)
                    .each(new CampaignPendingRewardEntity(
                            donation.getId(),
                            donation.getDonorSnapshot().getFullName(),
                            donation.getAmount(),
                            donation.getDonatedAt()
                    ));
            update.inc("pendingRewardsCount", 1);
        } else {
            // Donations without a reward are immediately considered concluded.
            update.push("concludedDonationIds", donation.getId());
        }

        // Apply the assembled update atomically to the campaign document.
        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(campaign.getId())),
                update,
                CampaignEntity.class
        );

        if (updateResult.getMatchedCount() == 0) {
            throw ApplicationErrorFactory.campaignNotFound(campaign.getId());
        }

    }

}
