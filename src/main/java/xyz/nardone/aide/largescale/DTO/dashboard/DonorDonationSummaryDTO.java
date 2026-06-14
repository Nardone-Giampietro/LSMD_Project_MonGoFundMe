package xyz.nardone.aide.largescale.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.nardone.aide.largescale.constant.EDonationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorDonationSummaryDTO {
    private String donationId;
    private String campaignId;
    private String campaignTitle;
    private LocalDateTime donatedAt;
    private BigDecimal amount;
    private EDonationStatus status;
}
