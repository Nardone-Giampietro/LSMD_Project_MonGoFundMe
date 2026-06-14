package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.constant.ECampaignTag;
import xyz.nardone.aide.largescale.entity.embedded.campaign.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "campaign")
public class CampaignEntity {

    @Id
    private String id;

    @Field("organizationId")
    private String organizationId;

    @Field("organizationName")
    private String organizationName;

    @Field("organizationCity")
    private String organizationCity;

    @Field("isOrganizationActive")
    private Boolean organizationActive;

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("thumbnailImageUrl")
    private String thumbnailImageUrl;

    @Field("tags")
    private List<ECampaignTag> tags;

    @Field("startDate")
    private LocalDateTime startDate;

    @Field("endDate")
    private LocalDateTime endDate;

    @Field("status")
    private ECampaignStatus status;

    @Field("goalAmount")
    private BigDecimal goalAmount;

    @Field("raisedAmount")
    private BigDecimal raisedAmount;

    @Field("donationsCount")
    private Integer donationsCount;

    @Field("pendingRewardsCount")
    private Integer pendingRewardsCount;

    @Field("milestonesTotalCount")
    private Integer milestonesTotalCount;

    @Field("verifiedMilestonesCount")
    private Integer verifiedMilestonesCount;

    @Field("latestDonations")
    private List<CampaignLatestDonationEntity> latestDonations;

    @Field("pendingRewards")
    private List<CampaignPendingRewardEntity> pendingRewards;

    @Field("concludedDonationIds")
    private List<String> concludedDonationIds;

    @Field("milestones")
    private List<CampaignMilestoneEntity> milestones;

    @Field("updates")
    private List<CampaignUpdateEntity> updates;

    @Field("availableRewards")
    private List<CampaignAvailableRewardEntity> availableRewards;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("updatedAt")
    private LocalDateTime updatedAt;
}
