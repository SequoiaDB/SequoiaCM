package com.sequoiacm.infrastructure.security.auth;

import com.sequoiacm.infrastructrue.security.core.ScmRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.List;

public class ScmAuthenticationHelper {

    private static final String ACTUATOR_ROLE_NAME = "ROLE_ACTUATOR";
    private static final String ACTUATOR_ROLE_ID = "ROLE_ACTUATOR";

    public static UsernamePasswordAuthenticationToken newAuthWithActuatorRole(
            UsernamePasswordAuthenticationToken auth) {
        List<GrantedAuthority> roles = new ArrayList<>(auth.getAuthorities());
        roles.add(new ScmRole(ACTUATOR_ROLE_ID, ACTUATOR_ROLE_NAME, "actuator role"));
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                auth.getPrincipal(), auth.getCredentials(), roles);
        newAuth.setDetails(auth.getDetails());
        return newAuth;
    }
}
