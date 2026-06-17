package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import xyz.nardone.aide.largescale.entity.DonationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends MongoRepository<DonationEntity, String> {

    Optional<DonationEntity> findByIdAndDonorId(String donationId, String donorId);

    Optional<DonationEntity> findByIdAndOrganizationId(String donationId, String organizationId);

    @Query("{ '_id': { '$in': ?0 }, 'organizationId': ?1, 'campaignId': ?2, 'status': 'CONCLUDED' }")
    List<DonationEntity> findConcludedDonationsByIds(List<String> donationIds, String organizationId, String campaignId);

    @Aggregation(pipeline = {
            "{ '$match': { 'donatedAt': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$campaignId', 'uniqueDonorIds': { '$addToSet': '$donorId' }, 'amountRaisedLast14Days': { '$sum': '$amount' } } }",
            "{ '$addFields': { 'uniqueDonorsLast14Days': { '$size': '$uniqueDonorIds' } } }",
            """
            {
              '$lookup': { 'from': 'campaign',
                'localField': '_id',
                'foreignField': '_id',
                'as': 'campaign'
              }
            }
            """,
            "{ '$unwind': '$campaign' }",
            "{ '$match': { 'campaign.status': 'OPEN' } }",
            "{ '$addFields': { 'relevanceScore': { '$add': [ { '$multiply': [ '$uniqueDonorsLast14Days', 3 ] }, " + "{ '$multiply': [ { '$ln': { '$add': [ '$amountRaisedLast14Days', 1 ] } }, 2 ] } ] } } }",
            "{ '$sort': { 'relevanceScore': -1, 'uniqueDonorsLast14Days': -1, 'amountRaisedLast14Days': -1 } }",
            "{ '$project': { '_id': 1 } }",
            "{ '$limit': ?1 }"
    })
    List<String> findRelevantOpenCampaigns(LocalDateTime donatedAfter, int limit);

}
