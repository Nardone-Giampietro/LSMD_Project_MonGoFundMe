package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.nardone.aide.largescale.entity.OrganizationPendingRewardAgingEntity;

import java.util.List;

public interface OrganizationPendingRewardAgingRepository
        extends MongoRepository<OrganizationPendingRewardAgingEntity, String> {

    @Aggregation(pipeline = {
            """
            {
              '$sort': {
                'riskScore': -1,
                'pendingOver30Days': -1,
                'pending15To30Days': -1,
                'pending8To14Days': -1,
                'pendingRewardsCount': -1
              }
            }
            """,
            "{ '$skip': ?0 }",
            "{ '$limit': ?1 }"
    })
    List<OrganizationPendingRewardAgingEntity> findRanking(long skip, int limit);
}
