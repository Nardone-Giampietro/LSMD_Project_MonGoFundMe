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
public class PersonSignupRequestDTO extends BaseSignupRequestDTO {

    @NotNull(message = "First Name is Required")
    private String firstName;
    @NotNull(message = "Last Name is Required")
    private String lastName;
}
