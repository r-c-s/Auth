package rcs.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import rcs.auth.utils.AuthUtils;

@Component
public class EndpointSecurity {

    private AuthUtils authUtils;

    public EndpointSecurity(AuthUtils authUtils) {
        this.authUtils = authUtils;
    }

    public boolean canUpdatePassword(Authentication authentication, String username) {
        User user = authUtils.tryGetLoggedInUser(authentication);
        return authUtils.isAdmin(user) || user.getUsername().equals(username);
    }
}
