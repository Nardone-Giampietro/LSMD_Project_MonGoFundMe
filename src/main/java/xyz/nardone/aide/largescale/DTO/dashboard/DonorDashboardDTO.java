package xyz.nardone.aide.largescale.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonorDashboardDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private List<DonorDonationSummaryDTO> donations;
}
