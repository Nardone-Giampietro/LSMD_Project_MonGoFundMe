package xyz.nardone.aide.largescale.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Donor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorGraphEntity {
    @Id
    private String id;
}
