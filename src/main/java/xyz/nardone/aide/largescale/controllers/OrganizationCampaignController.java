package xyz.nardone.aide.largescale.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.nardone.aide.largescale.DTO.campaign.*;
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.entity.CampaignEntity;
import xyz.nardone.aide.largescale.mapper.CampaignMapper;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;
import xyz.nardone.aide.largescale.service.interfaces.CampaignService;
import xyz.nardone.aide.largescale.util.CommonUtil;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/campaign")
@Tag(name = "Organization Campaigns", description = "Campaign APIs for logged in Organizations")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationCampaignController {

    private final CampaignService campaignService;
    private final CampaignMapper campaignMapper;
    private final CommonUtil commonUtil;

    public OrganizationCampaignController(CampaignService campaignService,
                                          CampaignMapper campaignMapper,
                                          CommonUtil commonUtil) {
        this.campaignService = campaignService;
        this.campaignMapper = campaignMapper;
        this.commonUtil = commonUtil;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Create Campaign", description = "Creates a Campaign for the logged in Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign created successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Request validation or business validation failed",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<String> createCampaign(@Valid @RequestBody CreateCampaignRequestDTO createCampaignRequestDTO) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        CampaignEntity savedCampaign = campaignService.createCampaign(
                createCampaignRequestDTO,
                loggedInOrganization.getId(),
                loggedInOrganization.getDisplayName(),
                loggedInOrganization.getLocation().getCity(),
                loggedInOrganization.getOrganizationStatus()
        );

        return ApiResponseDTO.success(
                "Campaign created successfully.",
                savedCampaign.getId()
        );
    }

    @GetMapping("/organization/closed-or-concluded")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Closed Or Concluded Campaigns",
            description = "Retrieves paginated closed or concluded Campaign summaries linked from the logged in Organization dashboard")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Closed or concluded Campaigns retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationCampaignSummaryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<OrganizationCampaignSummaryDTO>> getClosedOrConcludedCampaigns(@RequestParam(defaultValue = "0") int page,
                                                                                                @RequestParam(defaultValue = "20") int size) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        List<OrganizationCampaignSummaryDTO> campaigns = campaignService
                .findClosedOrConcludedOrganizationCampaigns(loggedInOrganization.getId(), page, size)
                .stream()
                .map(campaignMapper::toOrganizationCampaignSummaryDTO)
                .toList();

        return ApiResponseDTO.success(
                "Closed or concluded campaigns retrieved successfully.",
                campaigns
        );
    }

    @GetMapping("/organization/{campaignId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Organization Campaign",
            description = "Retrieves the private Campaign view for the owning Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization Campaign retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BaseCampaignDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campaign was not found or is not owned by the Organization",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<BaseCampaignDTO> getOrganizationCampaign(@PathVariable String campaignId) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        CampaignEntity campaign = campaignService.findOwnedByOrganization(campaignId, loggedInOrganization.getId());

        return ApiResponseDTO.success(
                "Organization campaign retrieved successfully.",
                campaignMapper.toOrganizationCampaignDTO(campaign)
        );
    }

    @PostMapping("/update/{campaignId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Add Campaign Update",
            description = "Adds a public update to an open Campaign owned by the logged in Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign update added successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Request validation failed, Campaign is not open or was not found, or update limit was reached",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> addCampaignUpdate(@PathVariable String campaignId,
                                                  @Valid @RequestBody CampaignUpdateRequestDTO updateDTO) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        campaignService.addUpdate(campaignId, loggedInOrganization.getId(), updateDTO);

        return ApiResponseDTO.success(
                "Campaign update added successfully.",
                null
        );
    }

    @DeleteMapping("/{campaignId}/update/{updateId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Delete Campaign Update",
            description = "Deletes an update from an open Campaign owned by the logged in Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign update deleted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campaign is not open or was not found, or Campaign update was not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> deleteCampaignUpdate(@PathVariable String campaignId,
                                                     @PathVariable String updateId) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        campaignService.deleteUpdate(campaignId, loggedInOrganization.getId(), updateId);

        return ApiResponseDTO.success(
                "Campaign update deleted successfully.",
                null
        );
    }

    @PostMapping("/milestone/verify")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Verify Campaign Milestone",
            description = "Verifies a milestone for an open Campaign owned by the logged in Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign milestone verified successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campaign is not open or was not found, or milestone was not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> verifyCampaignMilestone(@RequestParam String campaignId,
                                                        @RequestParam String milestoneId) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        campaignService.verifyMilestone(campaignId, loggedInOrganization.getId(), milestoneId);

        return ApiResponseDTO.success(
                "Campaign milestone verified successfully.",
                null
        );
    }
}
