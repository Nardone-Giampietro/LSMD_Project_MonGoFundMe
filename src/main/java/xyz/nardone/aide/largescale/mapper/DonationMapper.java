package xyz.nardone.aide.largescale.mapper;

import org.springframework.stereotype.Component;
import xyz.nardone.aide.largescale.DTO.donation.DonorDonationDTO;
import xyz.nardone.aide.largescale.DTO.donation.OrganizationDonationDTO;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignPendingRewardEntity;
import xyz.nardone.aide.largescale.entity.embedded.donation.DonationDonorAddressEntity;
import xyz.nardone.aide.largescale.entity.embedded.donation.DonationDonorSnapshotEntity;

@Component
public class DonationMapper {

    public DonorDonationDTO toDonorDonationDTO(DonationEntity donation) {
        DonationDonorAddressEntity donorAddress = donation.getDonorSnapshot().getAddress();

        return new DonorDonationDTO(
                donation.getId(),
                donation.getCampaignId(),
                donation.getCampaignTitle(),
                donation.getOrganizationLegalName(),
                donation.getAmount(),
                donation.getDonatedAt(),
                donation.getStatus(),
                donation.getReward() != null ? new DonorDonationDTO.RewardDTO(
                        donation.getReward().getRewardId(),
                        donation.getReward().getTitle()
                ) : null,
                donorAddress != null
                        ? new DonorDonationDTO.DonorAddressDTO(
                        donorAddress.getStreet(),
                        donorAddress.getCity(),
                        donorAddress.getZip()
                ) : null
        );
    }

    public OrganizationDonationDTO toOrganizationDonationDTO(DonationEntity donation) {
        DonationDonorSnapshotEntity donorSnapshot = donation.getDonorSnapshot();
        DonationDonorAddressEntity donorAddress = donorSnapshot.getAddress();

        return new OrganizationDonationDTO(
                donation.getId(),
                donorSnapshot.getFullName(),
                donorSnapshot.getEmail(),
                donorAddress != null
                        ? new OrganizationDonationDTO.DonorAddressDTO(
                        donorAddress.getStreet(),
                        donorAddress.getCity(),
                        donorAddress.getZip()
                ) : null,
                donation.getAmount(),
                donation.getDonatedAt(),
                donation.getStatus(),
                donation.getReward() != null ? new OrganizationDonationDTO.RewardDTO(
                        donation.getReward().getRewardId(),
                        donation.getReward().getTitle()
                ) : null
        );
    }

    public OrganizationDonationDTO toOrganizationDonationDTO(CampaignPendingRewardEntity pendingReward) {
        return new OrganizationDonationDTO(
                pendingReward.getDonationId(),
                pendingReward.getDonorName(),
                null,
                null,
                pendingReward.getAmount(),
                pendingReward.getDonatedAt(),
                null,
                null
        );
    }
}
