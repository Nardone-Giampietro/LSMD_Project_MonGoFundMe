package xyz.nardone.aide.largescale.DTO.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(PersonSignupRequestDTO.class),
        @JsonSubTypes.Type(OrganizationSignupRequestDTO.class)
})
public class BaseSignupRequestDTO {

    @Email
    @NotBlank(message = "Email is required")
    private String email;

    @NotEmpty(message = "Role is Required")
    private List<@NotBlank(message = "Role is Required") String> roles;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "ZIP is required")
    private String zip;
}
