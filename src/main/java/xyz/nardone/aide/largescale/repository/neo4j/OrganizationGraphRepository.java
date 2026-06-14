package xyz.nardone.aide.largescale.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import xyz.nardone.aide.largescale.entity.neo4j.OrganizationGraphEntity;

public interface OrganizationGraphRepository extends Neo4jRepository<OrganizationGraphEntity, String> {

    @Query("MERGE (organization:Organization {id: $organizationId}) RETURN organization")
    OrganizationGraphEntity createOrganization(@Param("organizationId") String organizationId);

    @Query("""
            MATCH (organization:Organization {id: $organizationId})
            OPTIONAL MATCH (organization)-[:CREATED]->(campaign:Campaign)
            WITH organization, collect(campaign) AS campaigns
            FOREACH (campaign IN campaigns | DETACH DELETE campaign)
            DETACH DELETE organization
            """)
    void deleteOrganizationSubgraph(@Param("organizationId") String organizationId);
}
