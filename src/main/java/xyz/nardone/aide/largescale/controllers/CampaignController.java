package xyz.nardone.aide.largescale.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import xyz.nardone.aide.largescale.DTO.campaign.CampaignSummaryDTO;
import xyz.nardone.aide.largescale.DTO.campaign.PublicCampaignDTO;
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.constant.ECampaignStatus;
import xyz.nardone.aide.largescale.constant.ECampaignTag;
import xyz.nardone.aide.largescale.constant.ESearchOrder;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.exception.ApplicationErrorFactory;
import xyz.nardone.aide.largescale.mapper.CampaignMapper;
import xyz.nardone.aide.largescale.service.interfaces.CampaignService;
import xyz.nardone.aide.largescale.service.interfaces.HomepageRecommendationService;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/campaign")
@Tag(name = "Campaigns", description = "Public Campaign APIs")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignMapper campaignMapper;
    private final HomepageRecommendationService homepageRecommendationService;

    public CampaignController(CampaignService campaignService,
                              CampaignMapper campaignMapper,
                              HomepageRecommendationService homepageRecommendationService) {
        this.campaignService = campaignService;
        this.campaignMapper = campaignMapper;
        this.homepageRecommendationService = homepageRecommendationService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search Campaigns", description = "Searches Campaigns with public filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign search completed successfully",
                    content = @Content(schema = @Schema(implementation = CampaignSummaryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<CampaignSummaryDTO>> searchCampaigns(@RequestParam(required = false) String text,
                                                                    @RequestParam(required = false) ECampaignTag tag,
                                                                    @RequestParam(required = false) ECampaignStatus status,
                                                                    @RequestParam(defaultValue = "NEWEST") ESearchOrder order,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        List<CampaignSummaryDTO> campaigns = campaignService
                .searchCampaigns(text, tag, status, order, page, size)
                .stream()
                .map(campaignMapper::toCampaignSummaryDTO)
                .toList();

        return ApiResponseDTO.success(
                "Campaign search completed successfully.",
                campaigns
        );
    }

    @GetMapping("/homepage")
    @Operation(summary = "Homepage Campaigns", description = "Retrieves the Campaign summaries selected for the homepage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Homepage Campaigns retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CampaignSummaryDTO.class))),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<CampaignSummaryDTO>> getHomepageCampaigns() {
        return ApiResponseDTO.success(
                "Homepage campaigns retrieved successfully.",
                homepageRecommendationService.findHomepageRecommendations()
        );
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Public Campaign", description = "Retrieves the public view of a Campaign")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PublicCampaignDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campaign was not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<PublicCampaignDTO> getPublicCampaign(@PathVariable String campaignId) {
        CampaignEntity campaign = campaignService.findById(campaignId)
                .orElseThrow(() -> ApplicationErrorFactory.campaignNotFound(campaignId));

        return ApiResponseDTO.success(
                "Campaign retrieved successfully.",
                campaignMapper.toPublicCampaignDTO(campaign)
        );
    }
}
