package xyz.nardone.aide.largescale.exception;

import xyz.nardone.aide.largescale.DTO.common.ErrorDTO;

public final class ApplicationErrorFactory {

    private ApplicationErrorFactory() {
    }

    public static ErrorDTO error(String code, String message) {
        return new ErrorDTO(code, message);
    }

    public static BusinessException business(String code, String message) {
        return new BusinessException(error(code, message));
    }

    public static BusinessException authError(String message) {
        return business("AUTH_ERROR", message);
    }

    public static BusinessException campaignNotFound(String campaignId) {
        return business("CAMPAIGN_NOT_FOUND", "Campaign not found for id " + campaignId + ".");
    }

    public static BusinessException organizationNotFound(String organizationId) {
        return business("ORGANIZATION_NOT_FOUND", "Organization not found for id " + organizationId + ".");
    }

    public static BusinessException organizationSuspended(String organizationId) {
        return business("ORGANIZATION_SUSPENDED", "Organization " + organizationId + " is suspended.");
    }

    public static BusinessException campaignUpdateLimitReached(String campaignId) {
        return business("CAMPAIGN_UPDATE_LIMIT_REACHED", "Campaign " + campaignId + " already has the maximum number of updates.");
    }

    public static BusinessException openCampaignLimitReached() {
        return business("OPEN_CAMPAIGN_LIMIT_REACHED", "An organization can have at most 5 open campaigns.");
    }

    public static BusinessException campaignUpdateNotFound(String updateId) {
        return business("CAMPAIGN_UPDATE_NOT_FOUND", "Campaign update not found for id " + updateId + ".");
    }

    public static BusinessException milestoneNotFound(String milestoneId) {
        return business("MILESTONE_NOT_FOUND", "Milestone not found for id " + milestoneId + ".");
    }

    public static BusinessException rewardNotFound(String rewardId) {
        return business("REWARD_NOT_FOUND", "Reward not found for id " + rewardId + ".");
    }

    public static BusinessException donationNotFound(String donationId) {
        return business("DONATION_NOT_FOUND", "Donation not found for id " + donationId + ".");
    }

}
