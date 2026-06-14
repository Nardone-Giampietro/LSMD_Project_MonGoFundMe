package xyz.nardone.aide.largescale.service;

import com.mongodb.bulk.BulkWriteResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import xyz.nardone.aide.largescale.DTO.dashboard.DonorDashboardDTO;
import xyz.nardone.aide.largescale.DTO.dashboard.DonorDonationSummaryDTO;
import xyz.nardone.aide.largescale.DTO.dashboard.OrganizationDashboardDTO;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.DashboardEntity;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.entity.DonorDashboardEntity;
import xyz.nardone.aide.largescale.entity.OrganizationDashboardEntity;
import xyz.nardone.aide.largescale.entity.embedded.LocationEntity;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.DonorDonationSummaryEntity;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.DonorSuggestedCampaignEntity;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.OrganizationOpenCampaignSummaryEntity;
import xyz.nardone.aide.largescale.exception.ApplicationErrorFactory;
import xyz.nardone.aide.largescale.repository.DashboardRepository;
import xyz.nardone.aide.largescale.repository.neo4j.SuggestedCampaignResult;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.CampaignGraphService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Maintains the denormalized dashboard read models.
 *
 * Donor and organization dashboards store embedded summaries so dashboard pages
 * can be served without joining donations, campaigns, and graph data on every
 * request. This service updates those summaries from user registration,
 * campaign creation, donation outbox events, and cached Neo4j recommendations.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;
    private final MongoTemplate mongoTemplate;
    private final CampaignGraphService campaignGraphService;

    @Value("${app.recommendation.donor-campaign-limit}")
    private int recommendationLimit;

    @Value("${app.recommendation.donor-campaign-expiration-days}")
    private long recommendationExpirationDays;

    public DashboardServiceImpl(DashboardRepository dashboardRepository,
                                MongoTemplate mongoTemplate,
                                CampaignGraphService campaignGraphService) {
        this.dashboardRepository = dashboardRepository;
        this.mongoTemplate = mongoTemplate;
        this.campaignGraphService = campaignGraphService;
    }

    @Override
    public void createDonorDashboard(String donorId) {
        DonorDashboardEntity dashboard = new DonorDashboardEntity(List.of(), List.of(), null);
        dashboard.setId(donorId);
        dashboardRepository.save(dashboard);
    }

    @Override
    public void createOrganizationDashboard(String organizationId) {
        OrganizationDashboardEntity dashboard = new OrganizationDashboardEntity(List.of(), List.of(), 0);
        dashboard.setId(organizationId);
        dashboardRepository.save(dashboard);
    }

    @Override
    public DonorDashboardDTO findDonorDashboard(UserDetailsImpl donor, int page, int size) {
        DonorDashboardEntity dashboard = findDonorDashboardById(donor.getId());
        LocationEntity location = donor.getLocation();
        String address = location.getStreet() + ", " + location.getZip() + " " + location.getCity();

        // Donation summaries are embedded on the dashboard, so only slice the requested page.
        List<DonorDonationSummaryDTO> donations = dashboard.getDonations().stream()
                .skip((long) page * size)
                .limit(size)
                .map(donation -> new DonorDonationSummaryDTO(
                        donation.getDonationId(),
                        donation.getCampaignId(),
                        donation.getCampaignTitle(),
                        donation.getDonatedAt(),
                        donation.getAmount(),
                        donation.getStatus()
                ))
                .toList();

        return new DonorDashboardDTO(
                donor.getFirstName(),
                donor.getLastName(),
                donor.getEmail(),
                address,
                donations
        );
    }

    @Override
    public OrganizationDashboardDTO findOrganizationDashboard(UserDetailsImpl organization) {
        OrganizationDashboardEntity dashboard = findOrganizationDashboardById(organization.getId());
        LocationEntity location = organization.getLocation();

        // Open campaign summaries are denormalized for fast dashboard rendering.
        List<OrganizationDashboardDTO.OpenCampaignDTO> openCampaigns = dashboard.getOpenCampaigns().stream()
                .map(campaign -> new OrganizationDashboardDTO.OpenCampaignDTO(
                        campaign.getCampaignId(),
                        campaign.getTitle(),
                        campaign.getStartDate(),
                        campaign.getEndDate(),
                        campaign.getRaisedAmount(),
                        campaign.getGoalAmount()
                ))
                .toList();

        return new OrganizationDashboardDTO(
                organization.getLegalName(),
                organization.getEmail(),
                location.getStreet(),
                location.getCity(),
                location.getZip(),
                dashboard.getOpenCampaignsCount(),
                openCampaigns
        );
    }

    @Override
    public void syncDonorDonationSummaries(List<DonationEntity> donations) {
        if (donations.isEmpty()) {
            return;
        }

        // Insert older events first so newest donations remain at the front.
        List<DonationEntity> orderedDonations = donations.stream()
                .sorted(Comparator.comparing(DonationEntity::getDonatedAt))
                .toList();

        BulkOperations addOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, DashboardEntity.class);
        for (DonationEntity donation : orderedDonations) {
            // Avoid duplicate summaries when an outbox event is retried.
            Query query = Query.query(Criteria.where("_id").is(donation.getDonorId())
                    .and("donations.donationId").ne(donation.getId()));
            Update update = new Update()
                    .push("donations")
                    .atPosition(0)
                    .each(new DonorDonationSummaryEntity(
                            donation.getId(),
                            donation.getCampaignId(),
                            donation.getCampaignTitle(),
                            donation.getDonatedAt(),
                            donation.getAmount(),
                            donation.getStatus()
                    ));
            addOperations.updateOne(query, update);
        }
        addOperations.execute();

        // A second pass updates statuses for existing summaries created by earlier runs.
        BulkOperations statusOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DashboardEntity.class);
        for (DonationEntity donation : donations) {
            Query query = Query.query(Criteria.where("_id").is(donation.getDonorId())
                    .and("donations.donationId").is(donation.getId()));
            Update update = new Update().set("donations.$.status", donation.getStatus());
            statusOperations.updateOne(query, update);
        }
        BulkWriteResult statusResult = statusOperations.execute();
        if (statusResult.getMatchedCount() < donations.size()) {
            throw new DataIntegrityViolationException("One or more donor dashboard documents are missing.");
        }
    }

    @Override
    public void syncDonorDonationStatuses(List<DonationEntity> donations) {
        if (donations.isEmpty()) {
            return;
        }

        // Status updates target the embedded summary created for each donation.
        BulkOperations statusOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DashboardEntity.class);
        for (DonationEntity donation : donations) {
            Query query = Query.query(Criteria.where("_id").is(donation.getDonorId())
                    .and("donations.donationId").is(donation.getId()));
            Update update = new Update().set("donations.$.status", donation.getStatus());
            statusOperations.updateOne(query, update);
        }

        BulkWriteResult statusResult = statusOperations.execute();
        if (statusResult.getMatchedCount() < donations.size()) {
            throw new DataIntegrityViolationException("One or more donation summaries are missing.");
        }
    }

    @Override
    public void refreshSuggestedCampaignsIfMissing(String donorId) {
        DonorDashboardEntity dashboard = findDonorDashboardById(donorId);
        if (dashboard.getSuggestedCampaignsExpiresAt() == null
                || !dashboard.getSuggestedCampaignsExpiresAt().isAfter(LocalDateTime.now())) {
            // Refresh only when the cached recommendation window has expired.
            refreshSuggestedCampaigns(donorId);
        }
    }

    @Override
    public List<SuggestedCampaignResult> findSuggestedCampaigns(String donorId) {
        DonorDashboardEntity dashboard = findDonorDashboardById(donorId);
        // Reuse cached graph recommendations until their expiration timestamp.
        List<DonorSuggestedCampaignEntity> suggestedCampaigns = dashboard.getSuggestedCampaignsExpiresAt() != null
                && dashboard.getSuggestedCampaignsExpiresAt().isAfter(LocalDateTime.now())
                ? dashboard.getSuggestedCampaigns()
                : refreshSuggestedCampaigns(donorId);

        return suggestedCampaigns.stream()
                .map(campaign -> new SuggestedCampaignResult(
                        campaign.getTitle(),
                        campaign.getThumbnailImageUrl()
                ))
                .toList();
    }

    @Override
    public void addOpenCampaign(CampaignEntity campaign) {
        // Keep a compact campaign summary on the organization dashboard.
        long updatedDashboards = dashboardRepository.addOpenCampaign(
                campaign.getOrganizationId(),
                new OrganizationOpenCampaignSummaryEntity(
                        campaign.getId(),
                        campaign.getTitle(),
                        campaign.getStartDate(),
                        campaign.getEndDate(),
                        campaign.getRaisedAmount(),
                        campaign.getGoalAmount()
                )
        );

        if (updatedDashboards == 0) {
            if (!dashboardRepository.existsById(campaign.getOrganizationId())) {
                throw new DataIntegrityViolationException(
                        "Dashboard document not found for organization " + campaign.getOrganizationId() + "."
                );
            }
            throw ApplicationErrorFactory.openCampaignLimitReached();
        }
    }

    @Override
    public void syncOpenCampaignRaisedAmounts(List<CampaignEntity> campaigns) {
        if (campaigns.isEmpty()) {
            return;
        }

        // Use max to avoid lowering a dashboard value when events arrive out of order.
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DashboardEntity.class);
        for (CampaignEntity campaign : campaigns) {
            Query query = Query.query(Criteria.where("_id").is(campaign.getOrganizationId())
                    .and("openCampaigns.campaignId").is(campaign.getId()));
            Update update = new Update().max("openCampaigns.$.raisedAmount", campaign.getRaisedAmount());
            bulkOperations.updateOne(query, update);
        }
        bulkOperations.execute();
    }

    @Override
    public void archiveOpenCampaign(String organizationId, String campaignId) {
        // Move the summary from the open list to the historical id list.
        long updatedDashboards = dashboardRepository.archiveOpenCampaign(organizationId, campaignId);

        if (updatedDashboards == 0) {
            throw new DataIntegrityViolationException(
                    "Open campaign summary not found in dashboard for campaign " + campaignId + "."
            );
        }
    }

    @Override
    public List<String> findOpenCampaignIds(String organizationId) {
        OrganizationDashboardEntity dashboard = findOrganizationDashboardById(organizationId);
        return dashboard.getOpenCampaigns().stream()
                .map(OrganizationOpenCampaignSummaryEntity::getCampaignId)
                .toList();
    }

    @Override
    public List<String> findClosedOrConcludedCampaignIds(String organizationId, int page, int size) {
        OrganizationDashboardEntity dashboard = findOrganizationDashboardById(organizationId);
        return dashboard.getClosedOrConcludedCampaignIds().stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    private DashboardEntity findByUserId(String userId) {
        // Dashboards are created together with users; missing documents are data integrity failures.
        return dashboardRepository.findById(userId)
                .orElseThrow(() -> new DataIntegrityViolationException(
                        "Dashboard document not found for user " + userId + "."
                ));
    }

    private DonorDashboardEntity findDonorDashboardById(String donorId) {
        DashboardEntity dashboard = findByUserId(donorId);
        if (!(dashboard instanceof DonorDashboardEntity donorDashboard)) {
            throw new DataIntegrityViolationException(
                    "Donor dashboard document not found for donor " + donorId + "."
            );
        }
        return donorDashboard;
    }

    private OrganizationDashboardEntity findOrganizationDashboardById(String organizationId) {
        DashboardEntity dashboard = findByUserId(organizationId);
        if (!(dashboard instanceof OrganizationDashboardEntity organizationDashboard)) {
            throw new DataIntegrityViolationException(
                    "Organization dashboard document not found for organization " + organizationId + "."
            );
        }
        return organizationDashboard;
    }

    private List<DonorSuggestedCampaignEntity> refreshSuggestedCampaigns(String donorId) {
        // Persist graph recommendations so dashboard reads do not always hit Neo4j.
        List<DonorSuggestedCampaignEntity> suggestedCampaigns = campaignGraphService
                .findSuggestedCampaigns(donorId, recommendationLimit)
                .stream()
                .map(campaign -> new DonorSuggestedCampaignEntity(
                        campaign.getTitle(),
                        campaign.getThumbnailImageUrl()
                ))
                .toList();

        long updatedDashboards = dashboardRepository.updateSuggestedCampaigns(
                donorId,
                suggestedCampaigns,
                LocalDateTime.now().plusDays(recommendationExpirationDays)
        );

        if (updatedDashboards == 0) {
            throw new DataIntegrityViolationException(
                    "Dashboard document not found for donor " + donorId + "."
            );
        }

        return suggestedCampaigns;
    }
}
