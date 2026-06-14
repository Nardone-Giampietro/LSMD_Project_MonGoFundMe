package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.aide.largescale.entity.DashboardEntity;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.DonorSuggestedCampaignEntity;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.OrganizationOpenCampaignSummaryEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardRepository extends MongoRepository<DashboardEntity, String> {

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'suggestedCampaigns': ?1, 'suggestedCampaignsExpiresAt': ?2 } }")
    long updateSuggestedCampaigns(String donorId,
                                  List<DonorSuggestedCampaignEntity> suggestedCampaigns,
                                  LocalDateTime expiresAt);

    @Query("{ '_id': ?0, 'openCampaignsCount': { '$lt': 5 } }")
    @Update("{ '$push': { 'openCampaigns': { '$each': [ ?1 ], '$position': 0 } }, '$inc': { 'openCampaignsCount': 1 } }")
    long addOpenCampaign(String organizationId, OrganizationOpenCampaignSummaryEntity campaign);

    @Query("{ '_id': ?0, 'openCampaigns.campaignId': ?1 }")
    @Update("{ '$pull': { 'openCampaigns': { 'campaignId': ?1 } }, '$push': { 'closedOrConcludedCampaignIds': { '$each': [ ?1 ], '$position': 0 } }, '$inc': { 'openCampaignsCount': -1 } }")
    long archiveOpenCampaign(String organizationId, String campaignId);
}
