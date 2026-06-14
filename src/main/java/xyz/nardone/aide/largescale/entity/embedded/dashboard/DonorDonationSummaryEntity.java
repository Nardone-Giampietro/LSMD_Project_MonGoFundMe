package xyz.nardone.aide.largescale.entity.embedded.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.constant.EDonationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorDonationSummaryEntity {

    @Field("donationId")
    private String donationId;

    @Field("campaignId")
    private String campaignId;

    @Field("campaignTitle")
    private String campaignTitle;

    @Field("donatedAt")
    private LocalDateTime donatedAt;

    @Field("amount")
    private BigDecimal amount;

    @Field("status")
    private EDonationStatus status;
}
