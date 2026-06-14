package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.DTO.donation.CreateDonationRequestDTO;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignPendingRewardEntity;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;

import java.util.List;

public interface DonationService {

    DonationEntity createDonation(CreateDonationRequestDTO requestDTO, UserDetailsImpl donor);

    DonationEntity findDonorDonation(String donationId, String donorId);

    DonationEntity findOrganizationDonation(String donationId, String organizationId);

    List<CampaignPendingRewardEntity> findOrganizationCampaignPendingRewards(String campaignId,
                                                                             String organizationId,
                                                                             int page,
                                                                             int size);

    List<DonationEntity> findOrganizationCampaignConcludedDonations(String campaignId,
                                                                    String organizationId,
                                                                    int page,
                                                                    int size);

    void concludeDonation(String donationId, String donorId);
}
