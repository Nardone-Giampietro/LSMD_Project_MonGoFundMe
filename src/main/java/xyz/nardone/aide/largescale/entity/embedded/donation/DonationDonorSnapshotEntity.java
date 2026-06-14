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
public class DonationDonorSnapshotEntity {

    @Field("fullName")
    private String fullName;

    @Field("email")
    private String email;

    @Field("address")
    private DonationDonorAddressEntity address;
}
