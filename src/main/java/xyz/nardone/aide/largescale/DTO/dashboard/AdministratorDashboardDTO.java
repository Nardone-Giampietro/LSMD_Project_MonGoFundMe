package xyz.nardone.aide.largescale.DTO.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdministratorDashboardDTO {
    private String firstName;
    private String lastName;
    private String email;
}
