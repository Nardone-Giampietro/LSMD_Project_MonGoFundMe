package xyz.nardone.aide.largescale.entity.embedded.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorSuggestedCampaignEntity {

    @Field("title")
    private String title;

    @Field("thumbnailImageUrl")
    private String thumbnailImageUrl;
}
