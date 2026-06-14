package xyz.nardone.aide.largescale.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.nardone.aide.largescale.DTO.campaign.CampaignSummaryDTO;
import xyz.nardone.aide.largescale.entity.HomepageRecommendationEntity;
import xyz.nardone.aide.largescale.mapper.CampaignMapper;
import xyz.nardone.aide.largescale.repository.CampaignRepository;
import xyz.nardone.aide.largescale.repository.DonationRepository;
import xyz.nardone.aide.largescale.repository.HomepageRecommendationRepository;
import xyz.nardone.aide.largescale.service.interfaces.HomepageRecommendationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds and serves the materialized homepage recommendation document.
 *
 * The daily refresh chooses which campaign ids should appear on the homepage,
 * while the summary refresh updates the stored campaign summary payloads for
 * those ids. Public homepage reads use this cached document directly instead of
 * querying the campaign collection on every request.
 */
@Service
public class HomepageRecommendationServiceImpl implements HomepageRecommendationService {

    private static final String DAILY_RECOMMENDATION_ID = "daily";

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final HomepageRecommendationRepository homepageRecommendationRepository;
    private final CampaignMapper campaignMapper;

    @Value("${app.scheduler.homepage-recommendation-lookback-days}")
    private int lookbackDays;

    @Value("${app.scheduler.homepage-recommendation-limit}")
    private int limit;

    public HomepageRecommendationServiceImpl(DonationRepository donationRepository,
                                             CampaignRepository campaignRepository,
                                             HomepageRecommendationRepository homepageRecommendationRepository,
                                             CampaignMapper campaignMapper) {
        this.donationRepository = donationRepository;
        this.campaignRepository = campaignRepository;
        this.homepageRecommendationRepository = homepageRecommendationRepository;
        this.campaignMapper = campaignMapper;
    }

    @Override
    public void refreshHomepageRecommendations() {
        // Select the daily campaign ids from recent donation activity.
        List<String> campaignIds = findRelevantOpenCampaignIds();
        List<CampaignSummaryDTO> campaigns = toCampaignSummaries(campaignIds);

        homepageRecommendationRepository.save(new HomepageRecommendationEntity(
                DAILY_RECOMMENDATION_ID,
                campaigns
        ));
    }

    @Override
    public void refreshHomepageRecommendationSummaries() {
        // Refresh only the summaries of the already selected daily campaigns.
        List<String> campaignIds = homepageRecommendationRepository
                .findById(DAILY_RECOMMENDATION_ID)
                .map(HomepageRecommendationEntity::getCampaigns)
                .orElse(List.of())
                .stream()
                .map(CampaignSummaryDTO::getCampaignId)
                .filter(Objects::nonNull)
                .toList();

        if (campaignIds.isEmpty()) {
            return;
        }

        List<CampaignSummaryDTO> campaigns = toCampaignSummaries(campaignIds);

        homepageRecommendationRepository.save(new HomepageRecommendationEntity(
                DAILY_RECOMMENDATION_ID,
                campaigns
        ));
    }

    @Override
    public List<CampaignSummaryDTO> findHomepageRecommendations() {
        // Public homepage reads use the materialized summary cache directly.
        return homepageRecommendationRepository
                .findById(DAILY_RECOMMENDATION_ID)
                .map(HomepageRecommendationEntity::getCampaigns)
                .orElse(List.of());
    }

    private List<String> findRelevantOpenCampaignIds() {
        return donationRepository.findRelevantOpenCampaigns(
                LocalDateTime.now().minusDays(lookbackDays),
                limit
        );
    }

    private List<CampaignSummaryDTO> toCampaignSummaries(List<String> campaignIds) {
        if (campaignIds.isEmpty()) {
            return List.of();
        }

        Map<String, CampaignSummaryDTO> summariesById = campaignRepository.findAllById(campaignIds)
                .stream()
                .map(campaignMapper::toCampaignSummaryDTO)
                .collect(Collectors.toMap(
                        CampaignSummaryDTO::getCampaignId,
                        Function.identity(),
                        (left, right) -> left
                ));

        // Preserve the ranking order produced by the recommendation query.
        return campaignIds.stream()
                .map(summariesById::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
