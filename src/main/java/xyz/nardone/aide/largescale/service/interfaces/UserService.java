package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.entity.UserEntity;

import java.util.Optional;

public interface UserService {

    Boolean existsByEmail(String email);

    <T extends UserEntity> T save(T user);

    Optional<UserEntity> findByEmail(String email);

    void suspendOrganization(String organizationId);
}
