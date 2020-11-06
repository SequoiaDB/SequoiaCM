package com.sequoiacm.infrastructure.tool.element;

public class ScmUserInfo {
    private String username;
    private String password;

    public ScmUserInfo(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
