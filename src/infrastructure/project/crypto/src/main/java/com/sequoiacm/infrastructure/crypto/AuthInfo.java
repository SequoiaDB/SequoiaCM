package com.sequoiacm.infrastructure.crypto;

public class AuthInfo {
    private String userName = "";
    private String password = "";
    private String encryptedPassword = "";

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

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("userName:").append(userName).append(",password:").append(password);
        return sb.toString();
    }
}
