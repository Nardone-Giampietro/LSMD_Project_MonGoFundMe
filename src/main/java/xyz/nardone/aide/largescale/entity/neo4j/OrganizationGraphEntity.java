package xyz.nardone.aide.largescale.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Organization")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationGraphEntity {
    @Id
    private String id;

    @Relationship(type = "CREATED", direction = Relationship.Direction.OUTGOING)
    private List<CampaignGraphEntity> campaigns = new ArrayList<>();
}
