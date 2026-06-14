package xyz.nardone.aide.largescale.service.neo4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.repository.neo4j.CampaignGraphRepository;
import xyz.nardone.aide.largescale.repository.neo4j.RecentExclusiveDonorCampaignResult;
import xyz.nardone.aide.largescale.repository.neo4j.SuggestedCampaignResult;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.CampaignGraphService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Store campaign-related Neo4j projection operations.
 *
 * This service creates campaign nodes, records donor-to-campaign donation
 * relationships, updates the graph-side campaign open flag, rebuilds similarity
 * relationships with GDS, and exposes graph-native recommendation and analytics
 * queries to the rest of the application.
 */
@Service
public class CampaignGraphServiceImpl implements CampaignGraphService {

    private final CampaignGraphRepository repository;

    public CampaignGraphServiceImpl(CampaignGraphRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public void createCampaign(String campaignId,
                               String organizationId,
                               String title,
                               String thumbnailImageUrl,
                               boolean open) {
        // The repository should create exactly one campaign node.
        long updatedNodes = repository.createCampaign(campaignId, organizationId, title, thumbnailImageUrl, open);
        if (updatedNodes != 1) {
            throw new IllegalStateException("Unable to create graph campaign.");
        }
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public void addDonation(String campaignId, String donorId, LocalDateTime lastDonatedAt) {
        // Donation relationships power donor-specific recommendations.
        long updatedRelationships = repository.addDonation(campaignId, donorId, lastDonatedAt);
        if (updatedRelationships != 1) {
            throw new IllegalStateException("Unable to create graph donation relationship.");
        }
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public void updateCampaignOpen(String campaignId, boolean open) {
        // Keep the graph-side open flag aligned with MongoDB campaign status changes.
        long updatedNodes = repository.updateCampaignOpen(campaignId, open);
        if (updatedNodes != 1) {
            throw new IllegalStateException("Unable to update graph campaign open state.");
        }
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public void refreshCampaignSimilarityScores(int topK, double similarityCutoff) {
        // Rebuild similarities from current campaign relationships before writing scores.
        repository.rebuildCampaignSimilarityGraph();
        repository.writeCampaignSimilarityScores(topK, similarityCutoff);
    }

    @Override
    public List<SuggestedCampaignResult> findSuggestedCampaigns(String donorId, int limit) {
        return repository.findSuggestedCampaigns(donorId, limit);
    }

    @Override
    public List<RecentExclusiveDonorCampaignResult> findRecentExclusiveDonorCampaigns(int limit) {
        // Administrator graph query for campaigns with recently exclusive donors.
        return repository.findRecentExclusiveDonorCampaigns(limit);
    }
}
