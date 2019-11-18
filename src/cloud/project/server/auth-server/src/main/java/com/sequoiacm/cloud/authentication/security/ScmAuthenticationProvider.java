package com.sequoiacm.cloud.authentication.security;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import com.sequoiacm.infrastructrue.security.core.ScmUser;

public class ScmAuthenticationProvider extends DaoAuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuthenticationProvider.class);

    private LdapTemplate ldap;
    private AuthenticationOptions authenticationOptions;

    public ScmAuthenticationProvider(LdapTemplate ldapTemplate, AuthenticationOptions authenticationOptions) {
        this.ldap = ldapTemplate;
        this.authenticationOptions = authenticationOptions;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        ScmUser user = (ScmUser) userDetails;
        switch (user.getPasswordType()) {
            case LDAP:
                ldapChecks(user, authentication);
                break;
            case LOCAL:
                super.additionalAuthenticationChecks(user, authentication);
                break;
            case TOKEN:
                tokenChecks(user, authentication);
                break;
            default:
                throw new InvalidUserPasswordTypeException(
                        String.format("User %s has invalid passwordType: %s",
                                user.getUsername(), user.getPasswordType()));
        }
    }

    private void ldapChecks(ScmUser user, UsernamePasswordAuthenticationToken authentication) {
        try {
            /*query().base("ou=users,ou=system")
                    .where("objectClass").is("person")
                    .and("uid").is(authentication.getName())*/
            LdapQuery query = query()
                    .where(authenticationOptions.getUsernameAttribute())
                    .is(authentication.getName());
            ldap.authenticate(
                    query,
                    (String) authentication.getCredentials()
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid LDAP username or password, user: " + authentication.getName(), e);
        }
    }

    private void tokenChecks(ScmUser user, UsernamePasswordAuthenticationToken authentication) {
        if (!authenticationOptions.isTokenEnabled()) {
            throw new BadCredentialsException("Token type is forbidden");
        }

        if (authenticationOptions.isTokenAllowAnyValue()) {
            return;
        }

        if (!authenticationOptions.getTokenValue().equals(authentication.getCredentials())) {
            throw new BadCredentialsException("Invalid token: " + authentication.getCredentials());
        }
    }
}
