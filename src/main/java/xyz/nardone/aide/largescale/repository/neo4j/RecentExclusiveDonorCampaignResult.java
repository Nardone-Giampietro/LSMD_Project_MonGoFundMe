package xyz.nardone.aide.largescale.repository.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentExclusiveDonorCampaignResult {
    private String campaignTitle;
    private Long recentExclusiveDonors;
}
