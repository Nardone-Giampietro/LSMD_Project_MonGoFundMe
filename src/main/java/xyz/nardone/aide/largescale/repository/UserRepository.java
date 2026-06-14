package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.aide.largescale.constant.EOrganizationStatus;
import xyz.nardone.aide.largescale.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {

    Optional<UserEntity> findUserEntitiesByEmail(String email);

    Boolean existsByEmail(String email);

    @Query("{ '_id': ?0, 'role.name': 'ROLE_ORGANIZATION' }")
    @Update("{ '$set': { 'status': ?1, 'updatedAt': ?2 } }")
    long updateOrganizationStatus(String organizationId, EOrganizationStatus status, LocalDateTime updatedAt);
}
