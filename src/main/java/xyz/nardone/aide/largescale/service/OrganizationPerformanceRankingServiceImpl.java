package xyz.nardone.aide.largescale.service;

import org.springframework.stereotype.Service;
import xyz.nardone.aide.largescale.DTO.analytics.OrganizationPerformanceDTO;
import xyz.nardone.aide.largescale.repository.CampaignRepository;
import xyz.nardone.aide.largescale.repository.OrganizationPerformanceRankingRepository;
import xyz.nardone.aide.largescale.service.interfaces.OrganizationPerformanceRankingService;

import java.util.List;

/**
 * Manages the administrator organization performance ranking.
 *
 * The refresh operation runs the MongoDB aggregation that computes performance
 * metrics and replaces the materialized ranking collection. Read operations
 * convert those ranking documents into the public analytics DTO returned by the
 * administrator endpoints.
 */
@Service
public class OrganizationPerformanceRankingServiceImpl implements OrganizationPerformanceRankingService {

    private final CampaignRepository campaignRepository;
    private final OrganizationPerformanceRankingRepository organizationPerformanceRankingRepository;

    public OrganizationPerformanceRankingServiceImpl(
            CampaignRepository campaignRepository,
            OrganizationPerformanceRankingRepository organizationPerformanceRankingRepository) {
        this.campaignRepository = campaignRepository;
        this.organizationPerformanceRankingRepository = organizationPerformanceRankingRepository;
    }

    @Override
    public void refresh() {
        // The aggregation computes ranking scores and replaces the materialized view.
        campaignRepository.refreshOrganizationPerformanceRanking();
    }

    @Override
    public List<OrganizationPerformanceDTO> findRanking(long skip, int limit) {
        // Convert ranking documents into the public administrator DTO shape.
        return organizationPerformanceRankingRepository.findRanking(skip, limit)
                .stream()
                .map(organization -> new OrganizationPerformanceDTO(
                        organization.getOrganizationId(),
                        organization.getOrganizationName(),
                        organization.getCity(),
                        organization.getCampaignsCount(),
                        organization.getOpenCampaignsCount(),
                        organization.getConcludedCampaignsCount(),
                        organization.getTotalRaisedAmount(),
                        organization.getTotalGoalAmount(),
                        organization.getTotalDonationsCount(),
                        organization.getFundingRatio(),
                        organization.getMilestoneVerificationRatio(),
                        organization.getPerformanceScore()
                ))
                .toList();
    }
}
