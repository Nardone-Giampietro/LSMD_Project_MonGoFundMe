package xyz.nardone.aide.largescale.service.neo4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.repository.neo4j.OrganizationGraphRepository;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.OrganizationGraphService;

/**
 * Maintains organization nodes and organization-owned graph subgraphs.
 *
 * Organization nodes are created before campaign nodes are attached to them.
 * When an organization is suspended, this service removes the organization
 * subgraph from Neo4j so recommendation and analytics reads stop considering
 * its campaigns.
 */
@Service
public class OrganizationGraphServiceImpl implements OrganizationGraphService {
    private final OrganizationGraphRepository organizationGraphRepository;

    public OrganizationGraphServiceImpl(OrganizationGraphRepository organizationGraphRepository) {
        this.organizationGraphRepository = organizationGraphRepository;
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public void createOrganization(String organizationId) {
        // Organization nodes are required before campaign graph nodes are attached.
        organizationGraphRepository.createOrganization(organizationId);
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public void deleteOrganizationSubgraph(String organizationId) {
        // Removing the organization also removes its graph-only campaign relationships.
        organizationGraphRepository.deleteOrganizationSubgraph(organizationId);
    }
}
