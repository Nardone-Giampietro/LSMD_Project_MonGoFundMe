package xyz.nardone.aide.largescale.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationEntity {
    @Field("city")
    private String city;
    @Field("street")
    private String street;
    @Field("zip")
    private String zip;
}
