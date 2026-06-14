package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.DonorDonationSummaryEntity;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.DonorSuggestedCampaignEntity;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DonorDashboardEntity extends DashboardEntity {

    @Field("donations")
    private List<DonorDonationSummaryEntity> donations;

    @Field("suggestedCampaigns")
    private List<DonorSuggestedCampaignEntity> suggestedCampaigns;

    @Field("suggestedCampaignsExpiresAt")
    private LocalDateTime suggestedCampaignsExpiresAt;
}
