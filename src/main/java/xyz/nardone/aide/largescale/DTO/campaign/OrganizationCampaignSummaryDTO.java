package xyz.nardone.aide.largescale.DTO.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationCampaignSummaryDTO {

    private String campaignId;
    private String title;
    private String thumbnailImageUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal goalAmount;
    private BigDecimal raisedAmount;
    private ECampaignStatus status;
}
