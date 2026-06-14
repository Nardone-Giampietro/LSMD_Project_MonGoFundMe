package xyz.nardone.aide.largescale.service.neo4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.repository.neo4j.DonorGraphRepository;
import xyz.nardone.aide.largescale.service.interfaces.neo4j.DonorGraphService;

/**
 * Maintains donor nodes in the Neo4j recommendation graph.
 *
 */
@Service
public class DonorGraphServiceImpl implements DonorGraphService {

    private final DonorGraphRepository donorRepository;

    public DonorGraphServiceImpl(DonorGraphRepository repository) {
        this.donorRepository = repository;
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public void createDonor(String donorId) {
        // Donor nodes are created from outbox events after MongoDB registration.
        donorRepository.createDonor(donorId);
    }
}
