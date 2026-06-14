package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "organization_pending_reward_aging")
public class OrganizationPendingRewardAgingEntity {

    @Id
    private String organizationId;

    @Field("organizationLegalName")
    private String organizationLegalName;

    @Field("pendingRewardsCount")
    private Long pendingRewardsCount;

    @Field("pending0To7Days")
    private Long pending0To7Days;

    @Field("pending8To14Days")
    private Long pending8To14Days;

    @Field("pending15To30Days")
    private Long pending15To30Days;

    @Field("pendingOver30Days")
    private Long pendingOver30Days;

    @Field("riskScore")
    private Double riskScore;

    @Field("generatedAt")
    private LocalDateTime generatedAt;
}
