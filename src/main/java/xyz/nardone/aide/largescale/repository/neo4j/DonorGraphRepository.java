package xyz.nardone.aide.largescale.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import xyz.nardone.aide.largescale.entity.neo4j.DonorGraphEntity;

public interface DonorGraphRepository extends Neo4jRepository<DonorGraphEntity, String> {

    @Query("MERGE (donor:Donor {id: $donorId}) RETURN donor")
    DonorGraphEntity createDonor(@Param("donorId") String donorId);
}
