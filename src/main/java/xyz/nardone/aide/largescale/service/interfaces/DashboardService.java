package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.DTO.dashboard.DonorDashboardDTO;
import xyz.nardone.aide.largescale.DTO.dashboard.OrganizationDashboardDTO;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.repository.neo4j.SuggestedCampaignResult;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;

import java.util.List;

public interface DashboardService {

    void createDonorDashboard(String donorId);

    void createOrganizationDashboard(String organizationId);

    DonorDashboardDTO findDonorDashboard(UserDetailsImpl donor, int page, int size);

    OrganizationDashboardDTO findOrganizationDashboard(UserDetailsImpl organization);

    void syncDonorDonationSummaries(List<DonationEntity> donations);

    void syncDonorDonationStatuses(List<DonationEntity> donations);

    void refreshSuggestedCampaignsIfMissing(String donorId);

    List<SuggestedCampaignResult> findSuggestedCampaigns(String donorId);

    void addOpenCampaign(CampaignEntity campaign);

    void syncOpenCampaignRaisedAmounts(List<CampaignEntity> campaigns);

    void archiveOpenCampaign(String organizationId, String campaignId);

    List<String> findOpenCampaignIds(String organizationId);

    List<String> findClosedOrConcludedCampaignIds(String organizationId, int page, int size);
}
