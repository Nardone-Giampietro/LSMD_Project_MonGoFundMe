package xyz.nardone.aide.largescale.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.DTO.dashboard.AdministratorDashboardDTO;
import xyz.nardone.aide.largescale.DTO.dashboard.DonorDashboardDTO;
import xyz.nardone.aide.largescale.DTO.dashboard.OrganizationDashboardDTO;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.util.CommonUtil;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboards", description = "Dashboard APIs for logged in users")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final CommonUtil commonUtil;
    private final DashboardService dashboardService;

    public DashboardController(CommonUtil commonUtil,
                               DashboardService dashboardService) {
        this.commonUtil = commonUtil;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/administrator")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Administrator Dashboard",
            description = "Retrieves the Administrator dashboard profile data from the logged in user details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrator dashboard retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AdministratorDashboardDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<AdministratorDashboardDTO> getAdministratorDashboard() {
        UserDetailsImpl loggedInAdministrator = commonUtil.loggedInUser();

        return ApiResponseDTO.success(
                "Administrator dashboard retrieved successfully.",
                new AdministratorDashboardDTO(
                        loggedInAdministrator.getFirstName(),
                        loggedInAdministrator.getLastName(),
                        loggedInAdministrator.getEmail()
                )
        );
    }

    @GetMapping("/donor")
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Donor Dashboard",
            description = "Retrieves the Donor profile data and a paginated page of stored donation summaries")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donor dashboard retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DonorDashboardDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<DonorDashboardDTO> getDonorDashboard(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        UserDetailsImpl loggedInDonor = commonUtil.loggedInUser();

        return ApiResponseDTO.success(
                "Donor dashboard retrieved successfully.",
                dashboardService.findDonorDashboard(loggedInDonor, page, size)
        );
    }

    @GetMapping("/organization")
    @PreAuthorize("hasRole('ORGANIZATION')")
    @Operation(summary = "Organization Dashboard",
            description = "Retrieves the Organization profile data and its embedded open Campaign summaries")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization dashboard retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationDashboardDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<OrganizationDashboardDTO> getOrganizationDashboard() {
        UserDetailsImpl loggedInOrganization = commonUtil.loggedInUser();

        return ApiResponseDTO.success(
                "Organization dashboard retrieved successfully.",
                dashboardService.findOrganizationDashboard(loggedInOrganization)
        );
    }
}
