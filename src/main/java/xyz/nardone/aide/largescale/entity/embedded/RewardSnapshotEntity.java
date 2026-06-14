package xyz.nardone.aide.largescale.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSnapshotEntity {

    @Field("rewardId")
    private String rewardId;

    @Field("title")
    private String title;
}
