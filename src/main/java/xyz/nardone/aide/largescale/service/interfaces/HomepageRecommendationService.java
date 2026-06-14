package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.DTO.campaign.CampaignSummaryDTO;

import java.util.List;

public interface HomepageRecommendationService {

    void refreshHomepageRecommendations();

    void refreshHomepageRecommendationSummaries();

    List<CampaignSummaryDTO> findHomepageRecommendations();
}
