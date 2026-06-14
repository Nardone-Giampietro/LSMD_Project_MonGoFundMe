package xyz.nardone.aide.largescale.config;

import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@Configuration
public class MongoConfiguration {

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory,
                                       MappingMongoConverter mappingMongoConverter) {
        // Reuse Spring's configured factory and converter for custom Mongo operations.
        return new MongoTemplate(mongoDatabaseFactory, mappingMongoConverter);
    }

    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
        // Enable explicit Mongo transaction manager selection in service methods.
        return new MongoTransactionManager(mongoDatabaseFactory);
    }

    @Bean
    public ApplicationRunner mongoIndexInitializer(MongoTemplate mongoTemplate) {
        return args -> ensureIndexes(mongoTemplate);
    }

    private void ensureIndexes(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("user_account")
                .ensureIndex(new Index()
                        .on("email", Sort.Direction.ASC)
                        .unique()
                        .named("user_account_email_unique_idx"));

        mongoTemplate.indexOps("campaign")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("status", 1)
                                .append("createdAt", -1))
                        .named("campaign_status_created_at_desc_idx"));

        mongoTemplate.indexOps("campaign")
                .ensureIndex(TextIndexDefinition.builder()
                        .named("campaign_title_description_text_idx")
                        .onField("title")
                        .onField("description")
                        .build());

        mongoTemplate.indexOps("campaign")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("tags", 1)
                                .append("status", 1)
                                .append("createdAt", -1))
                        .named("campaign_tags_status_created_at_desc_idx"));

        mongoTemplate.indexOps("donation")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("donatedAt", 1)
                                .append("campaignId", 1))
                        .named("donation_donated_at_campaign_id_idx"));

        mongoTemplate.indexOps("outbox_event")
                .ensureIndex(new CompoundIndexDefinition(
                        new Document("status", 1)
                                .append("eventType", 1)
                                .append("createdAt", 1))
                        .named("outbox_status_event_type_created_at_idx"));
    }
}
