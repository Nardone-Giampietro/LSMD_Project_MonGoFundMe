package xyz.nardone.aide.largescale.DTO.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.constant.ECampaignTag;
import xyz.nardone.aide.largescale.constant.EMilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseCampaignDTO {

    private String title;
    private ECampaignStatus status;
    private BigDecimal raisedAmount;
    private BigDecimal goalAmount;
    private String description;
    private String thumbnailImageUrl;
    private List<MilestoneDTO> milestones;
    private List<UpdateDTO> updates;
    private List<RewardDTO> rewards;
    private Integer totalDonators;
    private Integer rewardsCount;
    private List<ECampaignTag> tags;
    private String city;
    private String organizationName;
    private Integer pendingRewardsCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MilestoneDTO {
        private String milestoneId;
        private String title;
        private BigDecimal targetAmount;
        private EMilestoneStatus status;
        private LocalDateTime verificationDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RewardDTO {
        private String rewardId;
        private String title;
        private String description;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateDTO {
        private String updateId;
        @NotBlank(message = "Update title is required")
        @Size(max = 80, message = "Update title must be at most 80 characters")
        private String title;
        @NotBlank(message = "Update description is required")
        @Size(max = 300, message = "Update description must be at most 300 characters")
        private String description;
        private LocalDateTime date;
    }
}
