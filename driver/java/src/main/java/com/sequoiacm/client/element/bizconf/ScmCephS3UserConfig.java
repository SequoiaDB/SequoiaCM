package com.sequoiacm.client.element.bizconf;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;

/**
 * Ceph s3 data location user config class
 * 
 * @since 3.2.2
 */
public class ScmCephS3UserConfig {
    private String user;
    private String passwordFile;

    /**
     * Create ceph s3 user config with specified args.
     * 
     * @param user
     *            user
     * @param passwordFile
     *            password file
     * @throws ScmInvalidArgumentException
     *             if error happens.
     * @since 3.2.2
     */
    public ScmCephS3UserConfig(String user, String passwordFile)
            throws ScmInvalidArgumentException {
        checkValueNotNull(user, "user");
        checkValueNotNull(passwordFile, "passwordFile");
        this.user = user;
        this.passwordFile = passwordFile;
    }

    /**
     * Returns the user.
     * 
     * @return user.
     * @since 3.2.2
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user.
     * 
     * @param user
     *            user.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     * @since 3.2.2
     */
    public void setUser(String user) throws ScmInvalidArgumentException {
        checkValueNotNull(user, "user");
        this.user = user;
    }

    /**
     * Returns the password file.
     * 
     * @return password file.
     * @since 3.2.2
     */
    public String getPasswordFile() {
        return passwordFile;
    }

    /**
     * Sets the password file.
     * 
     * @param passwordFile
     *            password file.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     * @since 3.2.2
     */
    public void setPasswordFile(String passwordFile) throws ScmInvalidArgumentException {
        checkValueNotNull(passwordFile, "passwordFile");
        this.passwordFile = passwordFile;
    }

    private void checkValueNotNull(Object v, String argName) throws ScmInvalidArgumentException {
        if (v == null) {
            throw new ScmInvalidArgumentException("invalid arg:" + argName + " is null");
        }
    }
}
