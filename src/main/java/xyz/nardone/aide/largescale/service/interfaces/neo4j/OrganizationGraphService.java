package xyz.nardone.aide.largescale.service.interfaces.neo4j;

public interface OrganizationGraphService {

    void createOrganization(String organizationId);

    void deleteOrganizationSubgraph(String organizationId);
}
