package xyz.nardone.aide.largescale.DTO.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPendingRewardAgingDTO {

    private String organizationId;
    private String organizationLegalName;
    private Long pendingRewardsCount;
    private Long pending0To7Days;
    private Long pending8To14Days;
    private Long pending15To30Days;
    private Long pendingOver30Days;
    private Double riskScore;
}
