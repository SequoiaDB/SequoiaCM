package com.sequoiacm.cloud.authentication.security;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.infrastructure.security.sign.SignatureInfo;

public class ScmAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuthenticationProvider.class);

    private LdapTemplate ldap;
    private AuthenticationOptions authenticationOptions;

    private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";
    private PasswordEncoder passwordEncoder;
    private String userNotFoundEncodedPassword;

    private SaltSource saltSource;

    private ScmUserDetailsService userDetailsService;

    public ScmAuthenticationProvider(LdapTemplate ldapTemplate,
            AuthenticationOptions authenticationOptions, ScmUserDetailsService userDetailsService) {
        this.ldap = ldapTemplate;
        this.authenticationOptions = authenticationOptions;
        this.userDetailsService = userDetailsService;
        setPasswordEncoder(new PlaintextPasswordEncoder());
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        ScmUser user = (ScmUser) userDetails;
        ScmAuthenticationDetail detail = (ScmAuthenticationDetail) authentication.getDetails();
        if (detail.getSignatureInfo() != null) {
            signatureCheck(user, detail.getSignatureInfo());
            return;
        }
        switch (user.getPasswordType()) {
            case LDAP:
                ldapChecks(user, authentication);
                break;
            case LOCAL:
                localAuthenticationChecks(user, authentication);
                break;
            case TOKEN:
                tokenChecks(user, authentication);
                break;
            default:
                throw new InvalidUserPasswordTypeException(
                        String.format("User %s has invalid passwordType: %s", user.getUsername(),
                                user.getPasswordType()));
        }
    }

    private void signatureCheck(ScmUser user, SignatureInfo signatureInfo) {
        String signatrueFromServerKey = SignUtil.sign(signatureInfo.getAlgothm(),
                signatureInfo.getSecretKeyPrefix() + user.getSecretkey(),
                signatureInfo.getStringToSign());
        if (!signatrueFromServerKey.equals(signatureInfo.getSignature())) {
            logger.error("signature mismatch:serverSide={},request={},signatureInfo={}",
                    signatrueFromServerKey, signatureInfo.getSignature(), signatureInfo);
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
    }

    private void ldapChecks(ScmUser user, UsernamePasswordAuthenticationToken authentication) {
        try {
            /*
             * query().base("ou=users,ou=system")
             * .where("objectClass").is("person")
             * .and("uid").is(authentication.getName())
             */
            LdapQuery query = query().where(authenticationOptions.getUsernameAttribute())
                    .is(authentication.getName());
            ldap.authenticate(query, (String) authentication.getCredentials());
        }
        catch (Exception e) {
            throw new BadCredentialsException(
                    "Invalid LDAP username or password, user: " + authentication.getName(), e);
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

    @SuppressWarnings("deprecation")
    protected void localAuthenticationChecks(UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        Object salt = null;

        if (this.saltSource != null) {
            salt = this.saltSource.getSalt(userDetails);
        }

        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

        String presentedPassword = authentication.getCredentials().toString();

        if (!passwordEncoder.isPasswordValid(userDetails.getPassword(), presentedPassword, salt)) {
            logger.debug("Authentication failed: password does not match stored value");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
    }

    @Override
    protected void doAfterPropertiesSet() throws Exception {
        Assert.notNull(this.userDetailsService, "A UserDetailsService must be set");
    }

    @Override
    protected final UserDetails retrieveUser(String username,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        UserDetails loadedUser;

        ScmAuthenticationDetail detail = (ScmAuthenticationDetail) authentication.getDetails();
        try {
            if (!StringUtils.isEmpty(username) && !username.equals("NONE_PROVIDED")) {
                loadedUser = userDetailsService.loadUserByUsername(username);
                detail.setSignatureInfo(null);
            }
            else if (detail.getSignatureInfo() != null) {
                loadedUser = userDetailsService
                        .loadUserByAccesskey(detail.getSignatureInfo().getAccessKey());
            }
            else {
                throw new IllegalArgumentException(
                        "please specify username password or signature_info");
            }

        }
        catch (UsernameNotFoundException notFound) {
            logger.error("failed to login, user not found", notFound);
            if (authentication.getCredentials() != null) {
                String presentedPassword = authentication.getCredentials().toString();
                passwordEncoder.isPasswordValid(userNotFoundEncodedPassword, presentedPassword,
                        null);
            }
            throw notFound;
        }
        catch (Exception repositoryProblem) {
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(),
                    repositoryProblem);
        }

        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException(
                    "UserDetailsService returned null, which is an interface contract violation");
        }
        return loadedUser;
    }

    /**
     * Sets the PasswordEncoder instance to be used to encode and validate
     * passwords. If not set, the password will be compared as plain text.
     * <p>
     * For systems which are already using salted password which are encoded
     * with a previous release, the encoder should be of type
     * {@code org.springframework.security.authentication.encoding.PasswordEncoder}.
     * Otherwise, the recommended approach is to use
     * {@code org.springframework.security.crypto.password.PasswordEncoder}.
     *
     * @param passwordEncoder
     *            must be an instance of one of the {@code PasswordEncoder}
     *            types.
     */
    public void setPasswordEncoder(Object passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");

        if (passwordEncoder instanceof PasswordEncoder) {
            setPasswordEncoder((PasswordEncoder) passwordEncoder);
            return;
        }

        if (passwordEncoder instanceof org.springframework.security.crypto.password.PasswordEncoder) {
            final org.springframework.security.crypto.password.PasswordEncoder delegate = (org.springframework.security.crypto.password.PasswordEncoder) passwordEncoder;
            setPasswordEncoder(new PasswordEncoder() {
                @Override
                public String encodePassword(String rawPass, Object salt) {
                    checkSalt(salt);
                    return delegate.encode(rawPass);
                }

                @Override
                public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
                    checkSalt(salt);
                    return delegate.matches(rawPass, encPass);
                }

                private void checkSalt(Object salt) {
                    Assert.isNull(salt,
                            "Salt value must be null when used with crypto module PasswordEncoder");
                }
            });

            return;
        }

        throw new IllegalArgumentException("passwordEncoder must be a PasswordEncoder instance");
    }

    private void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");

        this.userNotFoundEncodedPassword = passwordEncoder.encodePassword(USER_NOT_FOUND_PASSWORD,
                null);
        this.passwordEncoder = passwordEncoder;
    }

    protected PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public void setSaltSource(SaltSource saltSource) {
        this.saltSource = saltSource;
    }

    protected SaltSource getSaltSource() {
        return saltSource;
    }

}
