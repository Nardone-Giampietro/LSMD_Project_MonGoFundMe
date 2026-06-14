package xyz.nardone.aide.largescale.entity.embedded.donation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DonationDonorAddressEntity {

    @Field("street")
    private String street;

    @Field("city")
    private String city;

    @Field("zip")
    private String zip;
}
