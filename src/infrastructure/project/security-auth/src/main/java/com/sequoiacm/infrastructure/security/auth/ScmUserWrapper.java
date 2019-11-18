package com.sequoiacm.infrastructure.security.auth;

import com.sequoiacm.infrastructrue.security.core.ScmUser;

public class ScmUserWrapper {
    private ScmUser user;
    private String userJSON;

    public ScmUserWrapper(ScmUser user, String userJSON) {
        this.user = user;
        this.userJSON = userJSON;
    }

    public ScmUser getUser() {
        return user;
    }

    public void setUser(ScmUser user) {
        this.user = user;
    }

    public String getUserJSON() {
        return userJSON;
    }

    public void setUserJSON(String userJSON) {
        this.userJSON = userJSON;
    }

    @Override
    public String toString() {
        return userJSON;
    }

}
