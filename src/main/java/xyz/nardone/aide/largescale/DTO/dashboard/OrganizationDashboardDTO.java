package xyz.nardone.aide.largescale.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDashboardDTO {
    private String legalName;
    private String email;
    private String street;
    private String city;
    private String zip;
    private Integer openCampaignsCount;
    private List<OpenCampaignDTO> openCampaigns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenCampaignDTO {
        private String campaignId;
        private String title;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal raisedAmount;
        private BigDecimal goalAmount;
    }
}
