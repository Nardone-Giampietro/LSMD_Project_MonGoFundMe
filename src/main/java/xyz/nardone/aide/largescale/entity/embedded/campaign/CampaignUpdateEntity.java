package xyz.nardone.aide.largescale.entity.embedded.campaign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignUpdateEntity {

    @Field("updateId")
    private String updateId;

    @Field("date")
    private LocalDateTime date;

    @Field("title")
    private String title;

    @Field("description")
    private String description;
}
