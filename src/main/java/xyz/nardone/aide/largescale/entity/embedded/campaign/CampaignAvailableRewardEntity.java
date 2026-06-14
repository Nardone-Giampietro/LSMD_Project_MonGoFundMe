package xyz.nardone.aide.largescale.entity.embedded.campaign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAvailableRewardEntity {

    @Field("rewardId")
    private String rewardId;

    @Field("title")
    private String title;

    @Field("description")
    private String description;

    @Field("amount")
    private BigDecimal amount;
}
