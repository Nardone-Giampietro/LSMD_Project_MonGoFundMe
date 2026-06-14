package xyz.nardone.aide.largescale.service.interfaces.neo4j;

import xyz.nardone.aide.largescale.repository.neo4j.RecentExclusiveDonorCampaignResult;
import xyz.nardone.aide.largescale.repository.neo4j.SuggestedCampaignResult;

import java.time.LocalDateTime;
import java.util.List;


public interface CampaignGraphService {

    void createCampaign(String campaignId, String organizationId, String title, String thumbnailImageUrl, boolean open);

    void addDonation(String campaignId, String donorId, LocalDateTime lastDonatedAt);

    void updateCampaignOpen(String campaignId, boolean open);

    void refreshCampaignSimilarityScores(int topK, double similarityCutoff);

    List<SuggestedCampaignResult> findSuggestedCampaigns(String donorId, int limit);

    List<RecentExclusiveDonorCampaignResult> findRecentExclusiveDonorCampaigns(int limit);
}
