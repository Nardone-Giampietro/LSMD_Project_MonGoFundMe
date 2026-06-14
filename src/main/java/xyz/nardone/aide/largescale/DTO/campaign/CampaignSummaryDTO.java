package xyz.nardone.aide.largescale.DTO.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.constant.ECampaignTag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignSummaryDTO {

    private String campaignId;
    private String title;
    private String thumbnailImageUrl;
    private List<ECampaignTag> tags;
    private ECampaignStatus status;
    private BigDecimal goalAmount;
    private BigDecimal raisedAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String organizationName;
    private String city;
    private Double score;
}
