package xyz.nardone.aide.largescale.entity.embedded.campaign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignLatestDonationEntity {

    @Field("donorName")
    private String donorName;

    @Field("amount")
    private BigDecimal amount;

    @Field("donatedAt")
    private LocalDateTime donatedAt;
}
