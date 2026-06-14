package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.DTO.analytics.OrganizationPendingRewardAgingDTO;

import java.util.List;

public interface OrganizationPendingRewardAgingService {

    void refresh();

    List<OrganizationPendingRewardAgingDTO> findRanking(long skip, int limit);
}
