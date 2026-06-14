package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.constant.EOrganizationStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationEntity extends UserEntity {

    @Field("legalName")
    private String legalName;

    @Field("status")
    private EOrganizationStatus status;
}
