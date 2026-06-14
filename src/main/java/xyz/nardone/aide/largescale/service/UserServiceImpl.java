package xyz.nardone.aide.largescale.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.aide.largescale.constant.EOrganizationStatus;
import xyz.nardone.aide.largescale.constant.EOutboxEventType;
import xyz.nardone.aide.largescale.entity.UserEntity;
import xyz.nardone.aide.largescale.exception.ApplicationErrorFactory;
import xyz.nardone.aide.largescale.repository.CampaignRepository;
import xyz.nardone.aide.largescale.repository.UserRepository;
import xyz.nardone.aide.largescale.service.interfaces.OutboxEventService;
import xyz.nardone.aide.largescale.service.interfaces.DashboardService;
import xyz.nardone.aide.largescale.service.interfaces.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides user persistence operations and organization suspension handling.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final OutboxEventService outboxEventService;
    private final DashboardService dashboardService;

    public UserServiceImpl(UserRepository userRepository,
                           CampaignRepository campaignRepository,
                           OutboxEventService outboxEventService,
                           DashboardService dashboardService) {
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.outboxEventService = outboxEventService;
        this.dashboardService = dashboardService;
    }

    @Override
    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public <T extends UserEntity> T save(T user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findUserEntitiesByEmail(email);
    }

    @Override
    @Transactional(transactionManager = "mongoTransactionManager")
    public void suspendOrganization(String organizationId) {
        LocalDateTime now = LocalDateTime.now();

        // Suspension is applied before closing campaigns so new writes see the new status.
        long updatedOrganizations = userRepository.updateOrganizationStatus(
                organizationId,
                EOrganizationStatus.SUSPENDED,
                now
        );

        if (updatedOrganizations == 0) {
            throw ApplicationErrorFactory.organizationNotFound(organizationId);
        }

        campaignRepository.deactivateOrganizationCampaigns(organizationId, now);

        List<String> openCampaignIds = dashboardService.findOpenCampaignIds(organizationId);
        if (!openCampaignIds.isEmpty()) {
            // Close every open campaign and remove it from the live dashboard list.
            campaignRepository.closeOpenCampaignsByIds(openCampaignIds, now);
            for (String campaignId : openCampaignIds) {
                dashboardService.archiveOpenCampaign(organizationId, campaignId);
            }
        }

        // Neo4j cleanup runs through the outbox after the Mongo transaction commits.
        outboxEventService.createEvent(
                EOutboxEventType.ORGANIZATION_SUSPENDED,
                Map.of("organizationId", organizationId)
        );
    }
}
