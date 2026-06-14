package xyz.nardone.aide.largescale.mapper;

import org.springframework.stereotype.Component;
import xyz.nardone.aide.largescale.constant.EDonationStatus;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.entity.embedded.LocationEntity;
import xyz.nardone.aide.largescale.entity.embedded.RewardSnapshotEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignAvailableRewardEntity;
import xyz.nardone.aide.largescale.entity.embedded.donation.DonationDonorAddressEntity;
import xyz.nardone.aide.largescale.entity.embedded.donation.DonationDonorSnapshotEntity;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;

import java.time.LocalDateTime;

@Component
public class DonationSnapshotMapper {

    public DonationEntity toDonationEntity(UserDetailsImpl donor,
                                           CampaignEntity campaign,
                                           CampaignAvailableRewardEntity selectedReward,
                                           java.math.BigDecimal amount,
                                           LocalDateTime donatedAt) {
        LocationEntity donorLocation = donor.getLocation();
        boolean rewardSelected = selectedReward != null;

        return new DonationEntity(
                null,
                donor.getId(),
                campaign.getOrganizationId(),
                campaign.getId(),
                campaign.getTitle(),
                campaign.getOrganizationName(),
                amount,
                donatedAt,
                rewardSelected ? EDonationStatus.PENDING : EDonationStatus.CONCLUDED,
                new DonationDonorSnapshotEntity(
                        donor.getDisplayName(),
                        donor.getEmail(),
                        rewardSelected ? new DonationDonorAddressEntity(
                                donorLocation.getStreet(),
                                donorLocation.getCity(),
                                donorLocation.getZip()
                        ) : null
                ),
                rewardSelected ? new RewardSnapshotEntity(
                        selectedReward.getRewardId(),
                        selectedReward.getTitle()
                ) : null
        );
    }
}
