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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.service.interfaces.UserService;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/v1/admin/organization")
@Tag(name = "Admin Organizations", description = "Administrator APIs for Organizations")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrganizationController {

    private final UserService userService;

    public AdminOrganizationController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/{organizationId}/suspend")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Suspend Organization",
            description = "Suspends an Organization and closes its open Campaigns")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization suspended successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Organization was not found",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> suspendOrganization(@PathVariable String organizationId) {
        userService.suspendOrganization(organizationId);

        return ApiResponseDTO.success(
                "Organization suspended successfully.",
                null
        );
    }
}
