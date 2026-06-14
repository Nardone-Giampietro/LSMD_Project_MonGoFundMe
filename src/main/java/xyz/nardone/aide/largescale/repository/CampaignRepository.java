package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.entity.embedded.campaign.CampaignUpdateEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends MongoRepository<CampaignEntity, String> {

    boolean existsByIdAndOrganizationIdAndStatus(String id, String organizationId, ECampaignStatus status);

    @Query(
            value = "{ '_id': ?0, 'status': 'OPEN' }",
            fields = "{ '_id': 1, 'organizationId': 1, 'organizationName': 1, 'title': 1, 'endDate': 1, 'raisedAmount': 1, 'milestones': 1, 'availableRewards': 1 }"
    )
    Optional<CampaignEntity> findCampaignForDonationCreationById(String campaignId);

    @Query(
            value = "{ '_id': { '$in': ?0 }, 'organizationId': ?1, 'status': { '$in': [ 'CLOSED', 'CONCLUDED' ] } }",
            fields = "{ '_id': 1, 'title': 1, 'thumbnailImageUrl': 1, 'startDate': 1, 'endDate': 1, 'goalAmount': 1, 'raisedAmount': 1, 'status': 1 }"
    )
    List<CampaignEntity> findClosedOrConcludedCampaignsByIds(List<String> campaignIds, String organizationId);

    @Query(
            value = "{ '_id': ?0, 'organizationId': ?1 }",
            fields = "{ 'pendingRewards': 1, 'concludedDonationIds': 1 }"
    )
    Optional<CampaignEntity> findDonationReadModelByIdAndOrganizationId(String campaignId, String organizationId);

    @Query("{ '_id': ?0, 'organizationId': ?1, 'status': 'OPEN', 'updates.99': { '$exists': false } }")
    @Update("{ '$push': { 'updates': ?2 }, '$set': { 'updatedAt': ?3 } }")
    long addUpdate(String campaignId,
                   String organizationId,
                   CampaignUpdateEntity campaignUpdateEntity,
                   LocalDateTime updatedAt);

    @Query("{ '_id': ?0, 'organizationId': ?1, 'status': 'OPEN', 'updates.updateId': ?2 }")
    @Update("{ '$pull': { 'updates': { 'updateId': ?2 } }, '$set': { 'updatedAt': ?3 } }")
    long deleteUpdate(String campaignId, String organizationId, String updateId, LocalDateTime updatedAt);

    @Query("{ '_id': ?0, 'organizationId': ?1, 'status': 'OPEN', 'milestones': { '$elemMatch': { 'milestoneId': ?2, 'status': 'PENDING' } } }")
    @Update("{ '$set': { 'milestones.$.status': 'VERIFIED', 'milestones.$.verificationDate': ?3, 'updatedAt': ?3 }, '$inc': { 'verifiedMilestonesCount': 1 } }")
    long verifyMilestone(String campaignId, String organizationId, String milestoneId, LocalDateTime updatedAt);

    @Query("{ '_id': ?0, 'pendingRewards.donationId': ?1 }")
    @Update("{ '$pull': { 'pendingRewards': { 'donationId': ?1 } }, '$push': { 'concludedDonationIds': ?1 }, '$inc': { 'pendingRewardsCount': -1 }, '$set': { 'updatedAt': ?2 } }")
    long movePendingRewardToConcludedDonations(String campaignId, String donationId, LocalDateTime updatedAt);

    @Query("{ '_id': { '$in': ?0 }, 'status': 'OPEN' }")
    @Update("{ '$set': { 'status': 'CLOSED', 'updatedAt': ?1 } }")
    void closeOpenCampaignsByIds(List<String> campaignIds, LocalDateTime updatedAt);

    @Query("{ 'organizationId': ?0 }")
    @Update("{ '$set': { 'isOrganizationActive': false, 'updatedAt': ?1 } }")
    long deactivateOrganizationCampaigns(String organizationId, LocalDateTime updatedAt);

    @Aggregation(pipeline = {
            """
            {
              '$match': {
                'status': 'OPEN',
                '$or': [
                  { '$expr': { '$gte': [ '$raisedAmount', '$goalAmount' ] } },
                  { 'endDate': { '$lte': ?0 } }
                ]
              }
            }
            """,
            "{ '$project': { '_id': 1, 'organizationId': 1 } }"
    })
    List<CampaignEntity> findOpenCampaignsToTransition(LocalDateTime now);

    @Query("""
            {
              'status': 'OPEN',
              '$or': [
                { '$expr': { '$gte': [ '$raisedAmount', '$goalAmount' ] } },
                { 'endDate': { '$lte': ?0 } }
              ]
            }
            """)
    @Update(pipeline = {
            """
            {
              '$set': {
                'status': {
                  '$cond': [
                    { '$gte': [ '$raisedAmount', '$goalAmount' ] },
                    'CONCLUDED',
                    'CLOSED'
                  ]
                },
                'updatedAt': ?0
              }
            }
            """
    })
    long transitionOpenCampaignStates(LocalDateTime now);

    @Aggregation(pipeline = {
            """
            {
              '$match': {
                'isOrganizationActive': true,
                'pendingRewardsCount': { '$gt': 0 }
              }
            }
            """,
            """
            {
              '$project': {
                'organizationId': 1,
                'organizationName': 1,
                'pendingRewardsCount': 1,
                'pending0To7Days': {
                  '$size': {
                    '$filter': {
                      'input': '$pendingRewards',
                      'as': 'reward',
                      'cond': {
                        '$gte': [
                          '$$reward.donatedAt',
                          { '$dateSubtract': { 'startDate': '$$NOW', 'unit': 'day', 'amount': 7 } }
                        ]
                      }
                    }
                  }
                },
                'pending8To14Days': {
                  '$size': {
                    '$filter': {
                      'input': '$pendingRewards',
                      'as': 'reward',
                      'cond': {
                        '$and': [
                          {
                            '$gte': [
                              '$$reward.donatedAt',
                              { '$dateSubtract': { 'startDate': '$$NOW', 'unit': 'day', 'amount': 14 } }
                            ]
                          },
                          {
                            '$lt': [
                              '$$reward.donatedAt',
                              { '$dateSubtract': { 'startDate': '$$NOW', 'unit': 'day', 'amount': 7 } }
                            ]
                          }
                        ]
                      }
                    }
                  }
                },
                'pending15To30Days': {
                  '$size': {
                    '$filter': {
                      'input': '$pendingRewards',
                      'as': 'reward',
                      'cond': {
                        '$and': [
                          {
                            '$gte': [
                              '$$reward.donatedAt',
                              { '$dateSubtract': { 'startDate': '$$NOW', 'unit': 'day', 'amount': 30 } }
                            ]
                          },
                          {
                            '$lt': [
                              '$$reward.donatedAt',
                              { '$dateSubtract': { 'startDate': '$$NOW', 'unit': 'day', 'amount': 14 } }
                            ]
                          }
                        ]
                      }
                    }
                  }
                },
                'pendingOver30Days': {
                  '$size': {
                    '$filter': {
                      'input': '$pendingRewards',
                      'as': 'reward',
                      'cond': {
                        '$lt': [
                          '$$reward.donatedAt',
                          { '$dateSubtract': { 'startDate': '$$NOW', 'unit': 'day', 'amount': 30 } }
                        ]
                      }
                    }
                  }
                }
              }
            }
            """,
            """
            {
              '$group': {
                '_id': '$organizationId',
                'organizationLegalName': { '$first': '$organizationName' },
                'pendingRewardsCount': { '$sum': '$pendingRewardsCount' },
                'pending0To7Days': { '$sum': '$pending0To7Days' },
                'pending8To14Days': { '$sum': '$pending8To14Days' },
                'pending15To30Days': { '$sum': '$pending15To30Days' },
                'pendingOver30Days': { '$sum': '$pendingOver30Days' }
              }
            }
            """,
            """
            {
              '$set': {
                'weightedRiskScore': {
                  '$add': [
                    '$pending0To7Days',
                    { '$multiply': [ '$pending8To14Days', 2 ] },
                    { '$multiply': [ '$pending15To30Days', 4 ] },
                    { '$multiply': [ '$pendingOver30Days', 8 ] }
                  ]
                }
              }
            }
            """,
            """
            {
              '$set': {
                'riskScore': {
                  '$multiply': [
                    {
                      '$divide': [
                        '$weightedRiskScore',
                        { '$multiply': [ '$pendingRewardsCount', 8 ] }
                      ]
                    },
                    100.0
                  ]
                },
                'generatedAt': '$$NOW'
              }
            }
            """,
            "{ '$unset': 'weightedRiskScore' }",
            "{ '$out': 'organization_pending_reward_aging' }"
    })
    void refreshOrganizationPendingRewardAging();

    @Aggregation(pipeline = {
            """
            {
              '$match': {
                'isOrganizationActive': true
              }
            }
            """,
            """
            {
              '$group': {
                '_id': '$organizationId',
                'organizationName': { '$first': '$organizationName' },
                'city': { '$first': '$organizationCity' },
                'campaignsCount': { '$sum': 1 },
                'openCampaignsCount': {
                  '$sum': { '$cond': [ { '$eq': [ '$status', 'OPEN' ] }, 1, 0 ] }
                },
                'concludedCampaignsCount': {
                  '$sum': { '$cond': [ { '$eq': [ '$status', 'CONCLUDED' ] }, 1, 0 ] }
                },
                'totalRaisedAmount': { '$sum': '$raisedAmount' },
                'totalGoalAmount': { '$sum': '$goalAmount' },
                'totalDonationsCount': {
                  '$sum': '$donationsCount'
                },
                'totalVerifiedMilestonesCount': {
                  '$sum': '$verifiedMilestonesCount'
                },
                'totalMilestonesCount': {
                  '$sum': '$milestonesTotalCount'
                }
              }
            }
            """,
            """
            {
              '$addFields': {
                'fundingRatio': {
                  '$cond': [
                    { '$gt': [ '$totalGoalAmount', 0 ] },
                    {
                      '$divide': [
                        { '$toDouble': '$totalRaisedAmount' },
                        { '$toDouble': '$totalGoalAmount' }
                      ]
                    },
                    0
                  ]
                },
                'milestoneVerificationRatio': {
                  '$cond': [
                    { '$gt': [ '$totalMilestonesCount', 0 ] },
                    { '$divide': [ '$totalVerifiedMilestonesCount', '$totalMilestonesCount' ] },
                    0
                  ]
                }
              }
            }
            """,
            """
            {
              '$addFields': {
                'performanceScore': {
                  '$add': [
                    { '$multiply': [ '$fundingRatio', 70 ] },
                    { '$multiply': [ '$milestoneVerificationRatio', 20 ] },
                    {
                      '$multiply': [
                        { '$ln': { '$add': [ '$totalDonationsCount', 1 ] } },
                        10
                      ]
                    }
                  ]
                }
              }
            }
            """,
            """
            {
              '$project': {
                '_id': 1,
                'organizationName': 1,
                'city': 1,
                'campaignsCount': 1,
                'openCampaignsCount': 1,
                'concludedCampaignsCount': 1,
                'totalRaisedAmount': 1,
                'totalGoalAmount': 1,
                'totalDonationsCount': 1,
                'fundingRatio': 1,
                'milestoneVerificationRatio': 1,
                'performanceScore': 1,
                'generatedAt': '$$NOW'
              }
            }
            """,
            "{ '$out': 'organization_performance_ranking' }"
    })
    void refreshOrganizationPerformanceRanking();
}
