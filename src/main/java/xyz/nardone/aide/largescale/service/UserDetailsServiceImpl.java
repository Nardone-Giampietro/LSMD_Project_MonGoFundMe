package xyz.nardone.aide.largescale.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import xyz.nardone.aide.largescale.entity.UserEntity;
import xyz.nardone.aide.largescale.service.interfaces.UserService;

/**
 * Loads application users for Spring Security authentication.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Spring Security passes the email as username for credential authentication.
        UserEntity user = userService
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found."));
        return UserDetailsImpl.build(user);
    }
}
