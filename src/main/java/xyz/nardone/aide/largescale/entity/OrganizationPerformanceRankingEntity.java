package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "organization_performance_ranking")
public class OrganizationPerformanceRankingEntity {

    @Id
    private String organizationId;

    @Field("organizationName")
    private String organizationName;

    @Field("city")
    private String city;

    @Field("campaignsCount")
    private Long campaignsCount;

    @Field("openCampaignsCount")
    private Long openCampaignsCount;

    @Field("concludedCampaignsCount")
    private Long concludedCampaignsCount;

    @Field("totalRaisedAmount")
    private BigDecimal totalRaisedAmount;

    @Field("totalGoalAmount")
    private BigDecimal totalGoalAmount;

    @Field("totalDonationsCount")
    private Long totalDonationsCount;

    @Field("fundingRatio")
    private Double fundingRatio;

    @Field("milestoneVerificationRatio")
    private Double milestoneVerificationRatio;

    @Field("performanceScore")
    private Double performanceScore;

    @Field("generatedAt")
    private LocalDateTime generatedAt;
}
