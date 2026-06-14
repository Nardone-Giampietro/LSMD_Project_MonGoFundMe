package xyz.nardone.aide.largescale.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.nardone.aide.largescale.DTO.analytics.OrganizationPendingRewardAgingDTO;
import xyz.nardone.aide.largescale.DTO.analytics.OrganizationPerformanceDTO;
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.repository.neo4j.RecentExclusiveDonorCampaignResult;
import xyz.nardone.aide.largescale.service.interfaces.CampaignService;
import xyz.nardone.aide.largescale.service.interfaces.OrganizationPendingRewardAgingService;
import xyz.nardone.aide.largescale.service.interfaces.OrganizationPerformanceRankingService;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/campaign/admin")
@Tag(name="Admin Analytics", description = "APIs for the analytic queries of the Administrator")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnalyticsController {

    private final CampaignService campaignService;
    private final OrganizationPendingRewardAgingService organizationPendingRewardAgingService;
    private final OrganizationPerformanceRankingService organizationPerformanceRankingService;

    public AdminAnalyticsController(CampaignService campaignService,
                                    OrganizationPendingRewardAgingService organizationPendingRewardAgingService,
                                    OrganizationPerformanceRankingService organizationPerformanceRankingService) {
        this.campaignService = campaignService;
        this.organizationPendingRewardAgingService = organizationPendingRewardAgingService;
        this.organizationPerformanceRankingService = organizationPerformanceRankingService;
    }

    @GetMapping("/organization-performance-ranking")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Performance Ranking", description = "Ranks the Organizations based on the performance of their Campaigns")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationPerformanceDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<OrganizationPerformanceDTO>> getOrganizationPerformanceRanking(
            @RequestParam(defaultValue = "0") long skip,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponseDTO.success(
                "Organization performance ranking retrieved successfully.",
                organizationPerformanceRankingService.findRanking(skip, limit)
        );
    }

    @GetMapping("/organization-pending-reward-aging-ranking")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Pending Reward Aging Ranking",
            description = "Ranks Organizations by the aging risk of their pending donations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationPendingRewardAgingDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Unexpected Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<OrganizationPendingRewardAgingDTO>> getOrganizationPendingRewardAgingRanking(
            @RequestParam(defaultValue = "0") long skip,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponseDTO.success(
                "Organization pending reward aging ranking retrieved successfully.",
                organizationPendingRewardAgingService.findRanking(skip, limit)
        );
    }

    @GetMapping("/recent-exclusive-donor-campaigns")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Recent Exclusive Donor Campaigns",
            description = "Finds open Campaigns ranked by donors that donated only to them during the recent period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RecentExclusiveDonorCampaignResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid limit parameter",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<RecentExclusiveDonorCampaignResult>> getRecentExclusiveDonorCampaigns(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponseDTO.success(
                "Recent exclusive donor campaigns retrieved successfully.",
                campaignService.findRecentExclusiveDonorCampaigns(limit)
        );
    }
}
