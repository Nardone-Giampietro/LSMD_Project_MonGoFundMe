package xyz.nardone.aide.largescale.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import xyz.nardone.aide.largescale.entity.neo4j.CampaignGraphEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface CampaignGraphRepository extends Neo4jRepository<CampaignGraphEntity, String> {

    @Query("""
            MATCH (organization:Organization {id: $organizationId})
            MERGE (campaign:Campaign {id: $campaignId})
            SET campaign.title = $title,
                campaign.thumbnailImageUrl = $thumbnailImageUrl,
                campaign.open = $open
            REMOVE campaign.status
            MERGE (organization)-[:CREATED]->(campaign)
            RETURN count(campaign)
            """)
    long createCampaign(@Param("campaignId") String campaignId,
                        @Param("organizationId") String organizationId,
                        @Param("title") String title,
                        @Param("thumbnailImageUrl") String thumbnailImageUrl,
                        @Param("open") boolean open);

    // Merge the relationship and keep the most recent donation timestamp.
    @Query("""
            MATCH (campaign:Campaign {id: $campaignId})
            MATCH (donor:Donor {id: $donorId})
            MERGE (campaign)-[donatedBy:DONATED_BY]->(donor)
            SET donatedBy.lastDonatedAt = CASE
                WHEN donatedBy.lastDonatedAt IS NULL OR donatedBy.lastDonatedAt < $lastDonatedAt
                THEN $lastDonatedAt
                ELSE donatedBy.lastDonatedAt
            END
            RETURN count(donatedBy)
            """)
    long addDonation(@Param("campaignId") String campaignId,
                     @Param("donorId") String donorId,
                     @Param("lastDonatedAt") LocalDateTime lastDonatedAt);

    @Query("""
            MATCH (campaign:Campaign {id: $campaignId})
            SET campaign.open = $open
            REMOVE campaign.status
            RETURN count(campaign)
            """)
    long updateCampaignOpen(@Param("campaignId") String campaignId, @Param("open") boolean open);

    @Query("""
            CALL gds.graph.drop('campaign-similarity-graph', false)
            YIELD graphName
            WITH count(graphName) AS droppedGraphs
            CALL gds.graph.project(
                'campaign-similarity-graph',
                ['Campaign', 'Donor'],
                {
                    DONATED_BY: {
                        type: 'DONATED_BY',
                        orientation: 'NATURAL'
                    }
                }
            )
            YIELD nodeCount
            RETURN nodeCount
            """)
    long rebuildCampaignSimilarityGraph();

    @Query("""
            MATCH ()-[similarity:SIMILAR_CAMPAIGN]->()
            DELETE similarity
            WITH count(similarity) AS deletedRelationships
            CALL gds.nodeSimilarity.filtered.write('campaign-similarity-graph', {
                sourceNodeFilter: 'Campaign',
                targetNodeFilter: 'Campaign',
                similarityMetric: 'JACCARD',
                topK: $topK,
                similarityCutoff: $similarityCutoff,
                writeRelationshipType: 'SIMILAR_CAMPAIGN',
                writeProperty: 'score'
            })
            YIELD relationshipsWritten
            FINISH
            """)
    void writeCampaignSimilarityScores(@Param("topK") int topK,
                                       @Param("similarityCutoff") double similarityCutoff);

    @Query("""
            MATCH (donor:Donor {id: $donorId})<-[:DONATED_BY]-(supportedCampaign:Campaign)
            MATCH (supportedCampaign)-[similarity:SIMILAR_CAMPAIGN]-(recommendedCampaign:Campaign {open: true})
            WHERE NOT (recommendedCampaign)-[:DONATED_BY]->(donor)
            WITH recommendedCampaign, max(similarity.score) AS recommendationScore
            RETURN recommendedCampaign.title AS title,
                   recommendedCampaign.thumbnailImageUrl AS thumbnailImageUrl
            ORDER BY recommendationScore DESC
            LIMIT $limit
            """)
    List<SuggestedCampaignResult> findSuggestedCampaigns(@Param("donorId") String donorId,
                                                         @Param("limit") int limit);

    @Query("""
            MATCH (campaign:Campaign {open: true})-[recentDonation:DONATED_BY]->(donor:Donor)
            WHERE recentDonation.lastDonatedAt >= localdatetime() - duration({days: 30})
              AND NOT EXISTS {
                  MATCH (otherCampaign:Campaign)-[:DONATED_BY]->(donor)
                  WHERE otherCampaign.id <> campaign.id
            }
            RETURN campaign.title AS campaignTitle,
                   count(DISTINCT donor) AS recentExclusiveDonors
            ORDER BY recentExclusiveDonors DESC
            LIMIT $limit
            """)
    List<RecentExclusiveDonorCampaignResult> findRecentExclusiveDonorCampaigns(@Param("limit") int limit);
}
