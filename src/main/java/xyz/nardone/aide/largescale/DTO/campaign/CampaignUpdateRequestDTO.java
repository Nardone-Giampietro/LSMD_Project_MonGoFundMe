package xyz.nardone.aide.largescale.DTO.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignUpdateRequestDTO {

    @NotBlank(message = "Update title is required")
    @Size(max = 150, message = "Update title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Update description is required")
    @Size(max = 300, message = "Update description must be at most 300 characters")
    private String description;
}
