package xyz.nardone.aide.largescale.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import xyz.nardone.aide.largescale.DTO.auth.BaseSignupRequestDTO;
import xyz.nardone.aide.largescale.DTO.auth.JwtResponseDTO;
import xyz.nardone.aide.largescale.DTO.auth.LoginRequestDTO;
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.constant.ERole;
import xyz.nardone.aide.largescale.service.JwtServiceImpl;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.service.interfaces.RegistrationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "APIs for authentication and user registration")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final RegistrationService registrationService;
    private final DashboardService dashboardService;
    private final JwtServiceImpl jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          RegistrationService registrationService,
                          DashboardService dashboardService,
                          JwtServiceImpl jwtService) {
        this.authenticationManager = authenticationManager;
        this.registrationService = registrationService;
        this.dashboardService = dashboardService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signin")
    @Operation(summary = "Sign In", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication completed successfully",
                    content = @Content(schema = @Schema(implementation = JwtResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Email or password is invalid",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<JwtResponseDTO>> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        // Delegate credential checks to Spring Security before issuing the JWT.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (roles.contains(ERole.ROLE_DONOR.toString())) {
            try {
                // Update donor recommendations during login without blocking authentication.
                dashboardService.refreshSuggestedCampaignsIfMissing(userDetails.getId());
            } catch (RuntimeException exception) {
                LOGGER.warn("Unable to refresh donor campaign recommendations during login.", exception);
            }
        }

        return ResponseEntity.ok(ApiResponseDTO.success(
                "Authentication successful.",
                new JwtResponseDTO(
                        jwt,
                        userDetails.getId(),
                        userDetails.getEmail(),
                        roles
                )
        ));
    }

    @PostMapping("/signup")
    @Operation(summary = "Sign Up", description = "Registers a new Donor, Organization, or Administrator account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Request or business validation failed",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Server error",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<Void>> registerUser(@Valid @RequestBody BaseSignupRequestDTO signupRequest) {
        registrationService.register(signupRequest);

        return new ResponseEntity<>(
                ApiResponseDTO.success(
                        "User registered successfully.",
                        null
                ),
                HttpStatus.CREATED
        );
    }
}
