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
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.DTO.donation.CreateDonationRequestDTO;
import xyz.nardone.aide.largescale.DTO.donation.DonorDonationDTO;
import xyz.nardone.aide.largescale.DTO.donation.OrganizationDonationDTO;
import xyz.nardone.aide.largescale.entity.DonationEntity;
import xyz.nardone.aide.largescale.mapper.DonationMapper;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;
import xyz.nardone.aide.largescale.service.interfaces.DonationService;
import xyz.nardone.aide.largescale.util.CommonUtil;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/donation")
@Tag(name = "Donations", description = "Donation APIs for Donors and Organizations")
@SecurityRequirement(name = "bearerAuth")
public class DonationController {

    private final DonationService donationService;
    private final CommonUtil commonUtil;
    private final DonationMapper donationMapper;

    public DonationController(DonationService donationService,
                              CommonUtil commonUtil,
                              DonationMapper donationMapper) {
        this.donationService = donationService;
        this.commonUtil = commonUtil;
        this.donationMapper = donationMapper;
    }

    @GetMapping("/{donationId}")
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Donor Donation Details",
            description = "Retrieves the full information of one Donation owned by the logged in Donor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DonorDonationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Donation was not found for the Donor",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<DonorDonationDTO> getDonorDonation(@PathVariable String donationId) {
        UserDetailsImpl loggedInDonor = commonUtil.loggedInUser();
        DonorDonationDTO donation = donationMapper.toDonorDonationDTO(
                donationService.findDonorDonation(donationId, loggedInDonor.getId())
        );

        return ApiResponseDTO.success(
                "Donation retrieved successfully.",
                donation
        );
    }

    @GetMapping("/organization/{donationId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Organization Donation Details",
            description = "Retrieves the full information of one Donation owned by the logged in Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationDonationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Donation was not found for the Organization",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<OrganizationDonationDTO> getOrganizationDonation(@PathVariable String donationId) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        OrganizationDonationDTO donation = donationMapper.toOrganizationDonationDTO(
                donationService.findOrganizationDonation(donationId, loggedInOrganization.getId())
        );

        return ApiResponseDTO.success(
                "Donation retrieved successfully.",
                donation
        );
    }

    @GetMapping("/campaign/{campaignId}/pending-rewards")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Campaign Pending Rewards",
            description = "Retrieves paginated pending reward summaries embedded in a Campaign owned by the logged in Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign pending rewards retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationDonationDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination parameters or Campaign was not found for the Organization",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<OrganizationDonationDTO>> getOrganizationCampaignPendingRewards(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        List<OrganizationDonationDTO> donations = donationService
                .findOrganizationCampaignPendingRewards(campaignId, loggedInOrganization.getId(), page, size)
                .stream()
                .map(donationMapper::toOrganizationDonationDTO)
                .toList();

        return ApiResponseDTO.success(
                "Campaign pending rewards retrieved successfully.",
                donations
        );
    }

    @GetMapping("/campaign/{campaignId}/concluded")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Campaign Concluded Donations",
            description = "Retrieves paginated concluded Donations linked from a Campaign owned by the logged in Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign concluded donations retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationDonationDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination parameters or Campaign was not found for the Organization",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<OrganizationDonationDTO>> getOrganizationCampaignConcludedDonations(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();
        List<OrganizationDonationDTO> donations = donationService
                .findOrganizationCampaignConcludedDonations(campaignId, loggedInOrganization.getId(), page, size)
                .stream()
                .map(donationMapper::toOrganizationDonationDTO)
                .toList();

        return ApiResponseDTO.success(
                "Campaign concluded donations retrieved successfully.",
                donations
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Create Donation",
            description = "Creates a donation from the logged in Donor to an open Campaign")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation created successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400",
                    description = "Request validation failed, Campaign was not found, or reward was not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<String> createDonation(@Valid @RequestBody CreateDonationRequestDTO requestDTO) {
        UserDetailsImpl loggedInDonor = commonUtil.loggedInUser();
        DonationEntity donation = donationService.createDonation(requestDTO, loggedInDonor);

        return ApiResponseDTO.success(
                "Donation created successfully.",
                donation.getId()
        );
    }

    @PatchMapping("/{donationId}/conclude")
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Conclude Donation",
            description = "Concludes a pending reward donation owned by the logged in Donor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation conclusion processed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campaign update failed after donation conclusion",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> concludeDonation(@PathVariable String donationId) {
        UserDetailsImpl loggedInDonor = commonUtil.loggedInUser();
        donationService.concludeDonation(donationId, loggedInDonor.getId());

        return ApiResponseDTO.success(
                "Donation concluded successfully.",
                null
        );
    }
}
