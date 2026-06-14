package xyz.nardone.aide.largescale.mapper;

import org.springframework.stereotype.Component;
import xyz.nardone.aide.largescale.DTO.campaign.*;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.constant.EMilestoneStatus;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CampaignMapper {

    public CampaignEntity toCampaignEntity(CreateCampaignRequestDTO requestDTO,
                                           String organizationId,
                                           String organizationDisplayName,
                                           String organizationCity) {
        LocalDateTime now = LocalDateTime.now();
        List<CampaignMilestoneEntity> milestones = toCampaignMilestones(requestDTO.getMilestones());

        return new CampaignEntity(
                null,
                organizationId,
                organizationDisplayName,
                organizationCity,
                true,
                requestDTO.getTitle(),
                requestDTO.getDescription(),
                requestDTO.getThumbnailImageUrl(),
                requestDTO.getTags(),
                now,
                requestDTO.getEndDate(),
                ECampaignStatus.OPEN,
                requestDTO.getObjective(),
                BigDecimal.ZERO,
                0,
                0,
                milestones.size(),
                0,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                milestones,
                new ArrayList<>(),
                toCampaignRewards(requestDTO.getRewards()),
                now,
                now
        );
    }

    public PublicCampaignDTO toPublicCampaignDTO(CampaignEntity campaignEntity) {
        PublicCampaignDTO campaignDTO = new PublicCampaignDTO();
        populateBaseCampaignDTO(campaignDTO, campaignEntity, false);
        campaignDTO.setLastDonations(toLastDonations(campaignEntity.getLatestDonations()));
        return campaignDTO;
    }

    public BaseCampaignDTO toOrganizationCampaignDTO(CampaignEntity campaignEntity) {
        BaseCampaignDTO campaignDTO = new BaseCampaignDTO();
        populateBaseCampaignDTO(campaignDTO, campaignEntity, true);
        campaignDTO.setPendingRewardsCount(campaignEntity.getPendingRewardsCount());
        return campaignDTO;
    }

    public OrganizationCampaignSummaryDTO toOrganizationCampaignSummaryDTO(CampaignEntity campaignEntity) {
        return new OrganizationCampaignSummaryDTO(
                campaignEntity.getId(),
                campaignEntity.getTitle(),
                campaignEntity.getThumbnailImageUrl(),
                campaignEntity.getStartDate(),
                campaignEntity.getEndDate(),
                campaignEntity.getGoalAmount(),
                campaignEntity.getRaisedAmount(),
                campaignEntity.getStatus()
        );
    }

    public CampaignSummaryDTO toCampaignSummaryDTO(CampaignEntity campaignEntity) {
        OrganizationCampaignSummaryDTO summaryDTO = toOrganizationCampaignSummaryDTO(campaignEntity);
        return new CampaignSummaryDTO(
                summaryDTO.getCampaignId(),
                summaryDTO.getTitle(),
                summaryDTO.getThumbnailImageUrl(),
                campaignEntity.getTags(),
                summaryDTO.getStatus(),
                summaryDTO.getGoalAmount(),
                summaryDTO.getRaisedAmount(),
                summaryDTO.getStartDate(),
                summaryDTO.getEndDate(),
                campaignEntity.getOrganizationName(),
                campaignEntity.getOrganizationCity(),
                null
        );
    }

    private List<CampaignMilestoneEntity> toCampaignMilestones(List<CreateCampaignRequestDTO.MilestoneDTO> milestoneDTOs) {
        if (milestoneDTOs == null) {
            return new ArrayList<>();
        }

        return milestoneDTOs.stream()
                .map(milestoneDTO -> new CampaignMilestoneEntity(
                        UUID.randomUUID().toString(),
                        milestoneDTO.getTitle(),
                        milestoneDTO.getTargetAmount(),
                        EMilestoneStatus.PENDING,
                        null
                ))
                .toList();
    }

    private List<CampaignAvailableRewardEntity> toCampaignRewards(List<CreateCampaignRequestDTO.RewardDTO> rewardDTOs) {
        if (rewardDTOs == null) {
            return new ArrayList<>();
        }

        return rewardDTOs.stream()
                .map(rewardDTO -> new CampaignAvailableRewardEntity(
                        UUID.randomUUID().toString(),
                        rewardDTO.getTitle(),
                        rewardDTO.getDescription(),
                        rewardDTO.getAmount()
                ))
                .toList();
    }

    private void populateBaseCampaignDTO(BaseCampaignDTO baseCampaignDTO,
                                         CampaignEntity campaignEntity,
                                         boolean includeUpdateId) {
        baseCampaignDTO.setTitle(campaignEntity.getTitle());
        baseCampaignDTO.setStatus(campaignEntity.getStatus());
        baseCampaignDTO.setRaisedAmount(campaignEntity.getRaisedAmount());
        baseCampaignDTO.setGoalAmount(campaignEntity.getGoalAmount());
        baseCampaignDTO.setDescription(campaignEntity.getDescription());
        baseCampaignDTO.setThumbnailImageUrl(campaignEntity.getThumbnailImageUrl());
        baseCampaignDTO.setMilestones(toBaseMilestones(campaignEntity.getMilestones(), includeUpdateId));
        baseCampaignDTO.setUpdates(toBaseUpdates(campaignEntity.getUpdates(), includeUpdateId));
        baseCampaignDTO.setRewards(toBaseRewards(campaignEntity.getAvailableRewards()));
        baseCampaignDTO.setTotalDonators(campaignEntity.getDonationsCount());
        baseCampaignDTO.setRewardsCount(campaignEntity.getAvailableRewards().size());
        baseCampaignDTO.setTags(campaignEntity.getTags());
        baseCampaignDTO.setCity(campaignEntity.getOrganizationCity());
        baseCampaignDTO.setOrganizationName(campaignEntity.getOrganizationName());
    }

    private List<BaseCampaignDTO.MilestoneDTO> toBaseMilestones(List<CampaignMilestoneEntity> milestoneEntities,
                                                                boolean includeMilestoneId) {
        return milestoneEntities.stream()
                .map(milestoneEntity -> new BaseCampaignDTO.MilestoneDTO(
                        includeMilestoneId ? milestoneEntity.getMilestoneId() : null,
                        milestoneEntity.getTitle(),
                        milestoneEntity.getTargetAmount(),
                        milestoneEntity.getStatus(),
                        milestoneEntity.getVerificationDate()
                ))
                .toList();
    }

    private List<BaseCampaignDTO.RewardDTO> toBaseRewards(List<CampaignAvailableRewardEntity> rewardEntities) {
        return rewardEntities.stream()
                .map(rewardEntity -> new BaseCampaignDTO.RewardDTO(
                        rewardEntity.getRewardId(),
                        rewardEntity.getTitle(),
                        rewardEntity.getDescription(),
                        rewardEntity.getAmount()
                ))
                .toList();
    }

    private List<BaseCampaignDTO.UpdateDTO> toBaseUpdates(List<CampaignUpdateEntity> updateEntities,
                                                          boolean includeUpdateId) {
        return updateEntities.stream()
                .map(updateEntity -> new BaseCampaignDTO.UpdateDTO(
                        includeUpdateId ? updateEntity.getUpdateId() : null,
                        updateEntity.getTitle(),
                        updateEntity.getDescription(),
                        updateEntity.getDate()
                ))
                .toList();
    }

    private List<PublicCampaignDTO.LastDonationDTO> toLastDonations(List<CampaignLatestDonationEntity> donations) {
        return donations.stream()
                .map(donation -> new PublicCampaignDTO.LastDonationDTO(
                        donation.getDonorName(),
                        donation.getAmount(),
                        donation.getDonatedAt()
                ))
                .toList();
    }

}
