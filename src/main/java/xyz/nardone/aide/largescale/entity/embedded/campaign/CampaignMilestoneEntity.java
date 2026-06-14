package xyz.nardone.aide.largescale.entity.embedded.campaign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.constant.EMilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMilestoneEntity {

    @Field("milestoneId")
    private String milestoneId;

    @Field("title")
    private String title;

    @Field("targetAmount")
    private BigDecimal targetAmount;

    @Field("status")
    private EMilestoneStatus status;

    @Field("verificationDate")
    private LocalDateTime verificationDate;
}
