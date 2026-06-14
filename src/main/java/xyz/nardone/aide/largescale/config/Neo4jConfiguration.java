package xyz.nardone.aide.largescale.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration
@EnableNeo4jRepositories(
        basePackages = "xyz.nardone.aide.largescale.repository.neo4j",
        transactionManagerRef = "neo4jTransactionManager"
)
public class Neo4jConfiguration {

    @Bean("neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver,
                                                           DatabaseSelectionProvider databaseSelectionProvider) {
        // Expose a named manager so graph services can isolate Neo4j transactions.
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }

    @Bean
    public ApplicationRunner neo4jIndexInitializer(Driver driver,
                                                   @Value("${spring.data.neo4j.database:}") String databaseName) {
        return args -> ensureIndexes(driver, databaseName);
    }

    private void ensureIndexes(Driver driver, String databaseName) {
        try (Session session = driver.session(sessionConfig(databaseName))) {
            session.run("""
                    CREATE CONSTRAINT organization_id IF NOT EXISTS
                    FOR (organization:Organization)
                    REQUIRE organization.id IS UNIQUE
                    """).consume();
            session.run("""
                    CREATE CONSTRAINT campaign_id IF NOT EXISTS
                    FOR (campaign:Campaign)
                    REQUIRE campaign.id IS UNIQUE
                    """).consume();
            session.run("""
                    CREATE CONSTRAINT donor_id IF NOT EXISTS
                    FOR (donor:Donor)
                    REQUIRE donor.id IS UNIQUE
                    """).consume();
            session.run("""
                    CREATE RANGE INDEX campaign_open_idx IF NOT EXISTS
                    FOR (campaign:Campaign)
                    ON (campaign.open)
                    """).consume();
            session.run("CALL db.awaitIndexes()").consume();
        }
    }

    private SessionConfig sessionConfig(String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            return SessionConfig.defaultConfig();
        }

        return SessionConfig.forDatabase(databaseName);
    }
}
