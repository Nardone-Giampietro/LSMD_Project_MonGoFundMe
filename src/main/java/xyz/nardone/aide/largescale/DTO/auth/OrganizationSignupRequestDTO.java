package xyz.nardone.aide.largescale.DTO.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationSignupRequestDTO extends BaseSignupRequestDTO {

    @NotNull(message = "Legal Name is Required")
    private String legalName;
}
