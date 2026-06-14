package xyz.nardone.aide.largescale.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.DTO.auth.BaseSignupRequestDTO;
import xyz.nardone.aide.largescale.DTO.auth.OrganizationSignupRequestDTO;
import xyz.nardone.aide.largescale.DTO.auth.PersonSignupRequestDTO;
import xyz.nardone.aide.largescale.constant.EOrganizationStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventType;
import xyz.nardone.aide.largescale.constant.ERole;
import xyz.nardone.aide.largescale.entity.AdministratorEntity;
import xyz.nardone.aide.largescale.entity.DonorEntity;
import xyz.nardone.aide.largescale.entity.OrganizationEntity;
import xyz.nardone.aide.largescale.entity.UserEntity;
import xyz.nardone.aide.largescale.entity.embedded.LocationEntity;
import xyz.nardone.aide.largescale.entity.embedded.RoleEntity;
import xyz.nardone.aide.largescale.exception.ApplicationErrorFactory;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.service.interfaces.OutboxEventService;
import xyz.nardone.aide.largescale.service.interfaces.RegistrationService;
import xyz.nardone.aide.largescale.service.interfaces.UserService;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles account registration for donors, organizations, and administrators.
 *
 * The service chooses the concrete user subtype from the requested role,
 * initializes shared account fields, hashes the password, and creates the
 * mandatory dashboard and graph outbox events for users that participate in
 * campaign or donation workflows.
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserService userService;
    private final OutboxEventService outboxEventService;
    private final DashboardService dashboardService;
    private final PasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(UserService userService,
                                   OutboxEventService outboxEventService,
                                   DashboardService dashboardService,
                                   PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.outboxEventService = outboxEventService;
        this.dashboardService = dashboardService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public UserEntity register(BaseSignupRequestDTO signupRequest) {
        // Email is the login identifier, so it must be unique across all user types.
        if (userService.existsByEmail(signupRequest.getEmail())) {
            throw ApplicationErrorFactory.authError("Email is already taken!");
        }

        ERole role = resolveRole(signupRequest);
        UserEntity user = createUser(signupRequest, role);
        LocalDateTime now = LocalDateTime.now();

        // Shared account fields are initialized after the concrete user type is chosen.
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setLocation(new LocationEntity(
                signupRequest.getCity(),
                signupRequest.getStreet(),
                signupRequest.getZip()
        ));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setRole(new RoleEntity(role));

        if (user instanceof OrganizationEntity organization) {
            return registerOrganization(organization);
        }
        if (user instanceof DonorEntity donor) {
            return registerDonor(donor);
        }

        return userService.save(user);
    }

    private OrganizationEntity registerOrganization(OrganizationEntity organization) {
        OrganizationEntity savedOrganization = userService.save(organization);
        // Organization dashboards and graph nodes are required for campaign workflows.
        dashboardService.createOrganizationDashboard(savedOrganization.getId());
        outboxEventService.createEvent(
                EOutboxEventType.ORGANIZATION_CREATED,
                Map.of("organizationId", savedOrganization.getId())
        );
        return savedOrganization;
    }

    private DonorEntity registerDonor(DonorEntity donor) {
        DonorEntity savedDonor = userService.save(donor);
        dashboardService.createDonorDashboard(savedDonor.getId());
        outboxEventService.createEvent(
                EOutboxEventType.DONOR_CREATED,
                Map.of("donorId", savedDonor.getId())
        );
        return savedDonor;
    }

    private UserEntity createUser(BaseSignupRequestDTO signupRequest, ERole role) {
        // The requested role determines which user subtype is valid for the payload.
        if (ERole.ROLE_ORGANIZATION.equals(role)) {
            if (!(signupRequest instanceof OrganizationSignupRequestDTO organizationSignupRequest)) {
                throw ApplicationErrorFactory.authError("Organization registration requires organization data.");
            }

            OrganizationEntity organization = new OrganizationEntity();
            organization.setLegalName(organizationSignupRequest.getLegalName());
            organization.setDisplayName(organizationSignupRequest.getLegalName());
            organization.setStatus(EOrganizationStatus.ACTIVE);
            return organization;
        }

        if (!(signupRequest instanceof PersonSignupRequestDTO personSignupRequest)) {
            throw ApplicationErrorFactory.authError("Donor and administrator registration require person data.");
        }

        String displayName = personSignupRequest.getFirstName() + " " + personSignupRequest.getLastName();
        if (ERole.ROLE_ADMINISTRATOR.equals(role)) {
            AdministratorEntity administrator = new AdministratorEntity();
            administrator.setFirstName(personSignupRequest.getFirstName());
            administrator.setLastName(personSignupRequest.getLastName());
            administrator.setDisplayName(displayName);
            return administrator;
        }

        DonorEntity donor = new DonorEntity();
        donor.setFirstName(personSignupRequest.getFirstName());
        donor.setLastName(personSignupRequest.getLastName());
        donor.setDisplayName(displayName);
        return donor;
    }

    private ERole resolveRole(BaseSignupRequestDTO signupRequest) {
        String requestedRole = signupRequest.getRoles().get(0);
        try {
            return ERole.valueOf(requestedRole);
        } catch (IllegalArgumentException exception) {
            throw ApplicationErrorFactory.authError("Invalid role: " + requestedRole + ".");
        }
    }
}
