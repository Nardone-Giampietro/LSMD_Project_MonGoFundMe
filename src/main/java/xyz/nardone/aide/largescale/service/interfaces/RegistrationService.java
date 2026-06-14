package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.DTO.auth.BaseSignupRequestDTO;
import xyz.nardone.aide.largescale.entity.UserEntity;

public interface RegistrationService {

    UserEntity register(BaseSignupRequestDTO signupRequest);
}
