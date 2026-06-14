package xyz.nardone.aide.largescale.service;

import org.springframework.stereotype.Service;
import xyz.nardone.aide.largescale.DTO.analytics.OrganizationPendingRewardAgingDTO;
import xyz.nardone.aide.largescale.repository.CampaignRepository;
import xyz.nardone.aide.largescale.repository.OrganizationPendingRewardAgingRepository;
import xyz.nardone.aide.largescale.service.interfaces.OrganizationPendingRewardAgingService;

import java.util.List;

/**
 * Manages the administrator pending reward aging ranking.
 *
 * The refresh operation delegates to the MongoDB aggregation that materializes
 * reward aging buckets and risk scores. Read operations then expose the stored
 * ranking through DTOs, keeping internal aggregation documents out of the API.
 */
@Service
public class OrganizationPendingRewardAgingServiceImpl implements OrganizationPendingRewardAgingService {

    private final CampaignRepository campaignRepository;
    private final OrganizationPendingRewardAgingRepository organizationPendingRewardAgingRepository;

    public OrganizationPendingRewardAgingServiceImpl(
            CampaignRepository campaignRepository,
            OrganizationPendingRewardAgingRepository organizationPendingRewardAgingRepository) {
        this.campaignRepository = campaignRepository;
        this.organizationPendingRewardAgingRepository = organizationPendingRewardAgingRepository;
    }

    @Override
    public void refresh() {
        // The aggregation writes the latest ranking into its materialized collection.
        campaignRepository.refreshOrganizationPendingRewardAging();
    }

    @Override
    public List<OrganizationPendingRewardAgingDTO> findRanking(long skip, int limit) {
        // Keep DTO mapping here so internal materialized-view fields stay private.
        return organizationPendingRewardAgingRepository.findRanking(skip, limit)
                .stream()
                .map(organization -> new OrganizationPendingRewardAgingDTO(
                        organization.getOrganizationId(),
                        organization.getOrganizationLegalName(),
                        organization.getPendingRewardsCount(),
                        organization.getPending0To7Days(),
                        organization.getPending8To14Days(),
                        organization.getPending15To30Days(),
                        organization.getPendingOver30Days(),
                        organization.getRiskScore()
                ))
                .toList();
    }
}
