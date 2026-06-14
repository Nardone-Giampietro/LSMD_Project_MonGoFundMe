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
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.repository.neo4j.SuggestedCampaignResult;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.util.CommonUtil;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/campaign")
@Tag(name = "Donor Campaigns", description = "Campaign APIs for logged in Donors")
@SecurityRequirement(name = "bearerAuth")
public class DonorCampaignController {

    private final DashboardService dashboardService;
    private final CommonUtil commonUtil;

    public DonorCampaignController(DashboardService dashboardService,
                                   CommonUtil commonUtil) {
        this.dashboardService = dashboardService;
        this.commonUtil = commonUtil;
    }

    @GetMapping("/suggested")
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Suggested Campaigns",
            description = "Retrieves the cached Campaign titles and thumbnails recommended to the logged in Donor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Suggested Campaigns retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SuggestedCampaignResult.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<SuggestedCampaignResult>> getSuggestedCampaigns() {
        UserDetailsImpl loggedInDonor = commonUtil.loggedInUser();

        return ApiResponseDTO.success(
                "Suggested campaigns retrieved successfully.",
                dashboardService.findSuggestedCampaigns(loggedInDonor.getId())
        );
    }
}
