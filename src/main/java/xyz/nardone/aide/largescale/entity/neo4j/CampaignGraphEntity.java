package xyz.nardone.aide.largescale.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Campaign")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignGraphEntity {
    @Id
    private String id;

    private String title;

    private String thumbnailImageUrl;

    private boolean open;

    @Relationship(type = "DONATED_BY", direction = Relationship.Direction.OUTGOING)
    private List<DonorGraphEntity> donatedBy = new ArrayList<>();
}
