package com.sequoiacm.client.core;

/**
 * Scm password type.
 */
public enum ScmUserPasswordType {
    /**
     * Local user password stores in local database
     */
    LOCAL,

    /**
     * LDAP user password stores in LDAP server
     */
    LDAP,

    /**
     * Token user use token to authenticate
     */
    TOKEN
}
