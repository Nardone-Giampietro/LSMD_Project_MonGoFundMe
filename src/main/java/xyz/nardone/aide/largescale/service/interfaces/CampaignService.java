package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.DTO.campaign.CampaignUpdateRequestDTO;
import xyz.nardone.aide.largescale.DTO.campaign.CreateCampaignRequestDTO;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.constant.ECampaignTag;
import xyz.nardone.aide.largescale.constant.EOrganizationStatus;
import xyz.nardone.aide.largescale.constant.ESearchOrder;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.repository.neo4j.RecentExclusiveDonorCampaignResult;

import java.util.List;
import java.util.Optional;

public interface CampaignService {

    CampaignEntity createCampaign(CreateCampaignRequestDTO createCampaignRequestDTO,
                                  String organizationId,
                                  String organizationDisplayName,
                                  String organizationCity,
                                  EOrganizationStatus organizationStatus);

    Optional<CampaignEntity> findById(String id);

    List<CampaignEntity> findClosedOrConcludedOrganizationCampaigns(String organizationId, int page, int size);

    List<CampaignEntity> searchCampaigns(String text,
                                         ECampaignTag tag,
                                         ECampaignStatus status,
                                         ESearchOrder order,
                                         int page,
                                         int size);

    List<RecentExclusiveDonorCampaignResult> findRecentExclusiveDonorCampaigns(int limit);

    void addUpdate(String campaignId, String organizationId, CampaignUpdateRequestDTO updateDTO);

    void deleteUpdate(String campaignId, String organizationId, String updateId);

    void verifyMilestone(String campaignId, String organizationId, String milestoneId);

    CampaignEntity findOwnedByOrganization(String campaignId, String organizationId);

    void transitionOpenCampaignStates();
}
