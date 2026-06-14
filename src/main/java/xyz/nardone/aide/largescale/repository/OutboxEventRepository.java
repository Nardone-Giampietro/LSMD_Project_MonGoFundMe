package xyz.nardone.aide.largescale.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.aide.largescale.constant.EOutboxEventStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventType;
import xyz.nardone.aide.largescale.entity.OutboxEventEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends MongoRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findByStatusAndEventTypeInOrderByCreatedAtAsc(
            EOutboxEventStatus status,
            Collection<EOutboxEventType> eventTypes
    );

    @Query("{ '_id': { '$in': ?0 }, 'status': 'PENDING' }")
    @Update("{ '$set': { 'status': 'PROCESSED', 'processedAt': ?1 } }")
    long markProcessedByIds(List<String> eventIds, LocalDateTime processedAt);

    long deleteByStatusAndProcessedAtBefore(EOutboxEventStatus status, LocalDateTime processedBefore);
}
