package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.DTO.analytics.OrganizationPerformanceDTO;

import java.util.List;

public interface OrganizationPerformanceRankingService {

    void refresh();

    List<OrganizationPerformanceDTO> findRanking(long skip, int limit);
}
