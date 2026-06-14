package xyz.nardone.aide.largescale.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import xyz.nardone.aide.largescale.exception.ApplicationErrorFactory;
import xyz.nardone.aide.largescale.service.UserDetailsImpl;

@Component
public class CommonUtil {
    public UserDetailsImpl loggedInUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        // Controllers expect the authenticated principal to be the application user details.
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw ApplicationErrorFactory.authError("Authenticated user is not available.");
        }
        return userDetails;
    }
}
