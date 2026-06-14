package xyz.nardone.aide.largescale.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPerformanceDTO {
    private String organizationId;
    private String organizationName;
    private String city;

    private Long campaignsCount;
    private Long openCampaignsCount;
    private Long concludedCampaignsCount;

    private BigDecimal totalRaisedAmount;
    private BigDecimal totalGoalAmount;
    private Long totalDonationsCount;

    private Double fundingRatio;
    private Double milestoneVerificationRatio;
    private Double performanceScore;
}
