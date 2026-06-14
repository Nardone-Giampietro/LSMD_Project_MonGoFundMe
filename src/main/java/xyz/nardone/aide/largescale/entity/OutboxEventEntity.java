package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.constant.EOutboxEventStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_event")
public class OutboxEventEntity {

    @Id
    private String id;

    @Field("eventType")
    private EOutboxEventType eventType;

    @Field("payload")
    private Map<String, Object> payload;

    @Field("status")
    private EOutboxEventStatus status;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("processedAt")
    private LocalDateTime processedAt;
}
