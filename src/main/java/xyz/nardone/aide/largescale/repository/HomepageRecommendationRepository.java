package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.nardone.aide.largescale.entity.HomepageRecommendationEntity;

public interface HomepageRecommendationRepository extends MongoRepository<HomepageRecommendationEntity, String> {
}
