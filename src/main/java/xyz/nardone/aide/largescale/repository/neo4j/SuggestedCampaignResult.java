package xyz.nardone.aide.largescale.repository.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedCampaignResult {
    private String title;
    private String thumbnailImageUrl;
}
