package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import xyz.nardone.aide.largescale.constant.EDonationStatus;
import xyz.nardone.aide.largescale.entity.embedded.RewardSnapshotEntity;
import xyz.nardone.aide.largescale.entity.embedded.donation.DonationDonorSnapshotEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "donation")
public class DonationEntity {

    @Id
    private String id;

    @Field("donorId")
    private String donorId;

    @Field("organizationId")
    private String organizationId;

    @Field(name = "campaignId", targetType = FieldType.OBJECT_ID)
    private String campaignId;

    @Field("campaignTitle")
    private String campaignTitle;

    @Field("organizationLegalName")
    private String organizationLegalName;

    @Field("amount")
    private BigDecimal amount;

    @Field("donatedAt")
    private LocalDateTime donatedAt;

    @Field("status")
    private EDonationStatus status;

    @Field("donorSnapshot")
    private DonationDonorSnapshotEntity donorSnapshot;

    @Field("reward")
    private RewardSnapshotEntity reward;
}
