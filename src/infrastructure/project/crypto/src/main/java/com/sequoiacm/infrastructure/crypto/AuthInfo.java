package com.sequoiacm.infrastructure.crypto;

public class AuthInfo {
    private String userName = "";
    private String password = "";

    public AuthInfo() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("userName:").append(userName).append(",password:").append(password);
        return sb.toString();
    }
}
