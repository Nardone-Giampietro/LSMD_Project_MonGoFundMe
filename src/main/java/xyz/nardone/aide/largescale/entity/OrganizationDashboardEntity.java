package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.entity.embedded.dashboard.OrganizationOpenCampaignSummaryEntity;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationDashboardEntity extends DashboardEntity {

    @Field("openCampaigns")
    private List<OrganizationOpenCampaignSummaryEntity> openCampaigns;

    @Field("closedOrConcludedCampaignIds")
    private List<String> closedOrConcludedCampaignIds;

    @Field("openCampaignsCount")
    private Integer openCampaignsCount;
}
