package xyz.nardone.aide.largescale.entity.embedded.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationOpenCampaignSummaryEntity {

    @Field("campaignId")
    private String campaignId;

    @Field("title")
    private String title;

    @Field("startDate")
    private LocalDateTime startDate;

    @Field("endDate")
    private LocalDateTime endDate;

    @Field("raisedAmount")
    private BigDecimal raisedAmount;

    @Field("goalAmount")
    private BigDecimal goalAmount;
}
