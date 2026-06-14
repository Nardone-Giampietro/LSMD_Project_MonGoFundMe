package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.nardone.aide.largescale.entity.OrganizationPerformanceRankingEntity;

import java.util.List;

public interface OrganizationPerformanceRankingRepository
        extends MongoRepository<OrganizationPerformanceRankingEntity, String> {

    @Aggregation(pipeline = {
            """
            {
              '$sort': {
                'performanceScore': -1,
                'totalRaisedAmount': -1,
                'totalDonationsCount': -1
              }
            }
            """,
            "{ '$skip': ?0 }",
            "{ '$limit': ?1 }"
    })
    List<OrganizationPerformanceRankingEntity> findRanking(long skip, int limit);
}
