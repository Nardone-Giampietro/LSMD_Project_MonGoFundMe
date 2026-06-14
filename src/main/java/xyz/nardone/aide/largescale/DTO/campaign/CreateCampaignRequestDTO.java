package xyz.nardone.aide.largescale.DTO.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.nardone.aide.largescale.constant.ECampaignTag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateCampaignRequestDTO {

    @NotBlank(message = "Campaign title is required")
    private String title;

    @NotNull(message = "Objective is required")
    @DecimalMin(value = "1.00", message = "Objective must be at least 1.00")
    private BigDecimal objective;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Thumbnail image URL is required")
    private String thumbnailImageUrl;

    @NotEmpty(message = "At least one tag is required")
    @Size(max = 3, message = "At most 3 tags are allowed")
    private List<ECampaignTag> tags;

    @Valid
    @Size(max = 50, message = "At most 50 milestones are allowed")
    private List<MilestoneDTO> milestones;

    @Valid
    @Size(max = 120, message = "At most 120 rewards are allowed")
    private List<RewardDTO> rewards;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MilestoneDTO {

        @NotBlank(message = "Milestone title is required")
        @Size(max = 150, message = "Milestone title must be at most 150 characters")
        private String title;

        @NotNull(message = "Milestone target amount is required")
        @DecimalMin(value = "1.00", message = "Milestone target amount must be at least 1.00")
        private BigDecimal targetAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RewardDTO {

        @NotBlank(message = "Reward title is required")
        @Size(max = 150, message = "Reward title must be at most 150 characters")
        private String title;

        @NotBlank(message = "Reward description is required")
        @Size(max = 500, message = "Reward description must be at most 500 characters")
        private String description;

        @NotNull(message = "Reward amount is required")
        @DecimalMin(value = "1.00", message = "Reward amount must be at least 1.00")
        private BigDecimal amount;
    }
}
