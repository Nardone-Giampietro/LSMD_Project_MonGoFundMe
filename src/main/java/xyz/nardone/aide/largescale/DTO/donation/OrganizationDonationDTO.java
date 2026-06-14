package xyz.nardone.aide.largescale.DTO.donation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.nardone.aide.largescale.constant.EDonationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationDonationDTO {

    private String donationId;
    private String donorName;
    private String donorEmail;
    private DonorAddressDTO donorAddress;
    private BigDecimal amount;
    private LocalDateTime donatedAt;
    private EDonationStatus status;
    private RewardDTO reward;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RewardDTO {
        private String rewardId;
        private String title;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DonorAddressDTO {
        private String street;
        private String city;
        private String zip;
    }
}
