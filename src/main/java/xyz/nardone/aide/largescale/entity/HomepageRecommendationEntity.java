package xyz.nardone.aide.largescale.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import xyz.nardone.aide.largescale.DTO.campaign.CampaignSummaryDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "homepage_recommendation")
public class HomepageRecommendationEntity {

    @Id
    private String id;

    @Field("campaigns")
    private List<CampaignSummaryDTO> campaigns;
}
