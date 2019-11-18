package com.sequoiacm.perf.config;

import com.sequoiacm.perf.common.ApiMethod;
import com.sequoiacm.perf.common.ApiType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class Config {
    private String driverUrl;
    private String restUrl;
    private String user;
    private String password;
    private ApiType apiType;
    private ApiMethod apiMethod;
    private String workspace;
    private int threadNum;
    private int fileSize;
    private int fileNum;
    private String fileDownloadPath;
    private boolean readFromDisk;
    private String fileReadPath;


    public void setDriverUrl(String driverUrl) {
        this.driverUrl = driverUrl;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setApiType(String apiType) {
        this.apiType = ApiType.valueOf(apiType.toUpperCase());
    }

    public void setApiMethod(String apiMethod) {
        this.apiMethod = ApiMethod.valueOf(apiMethod.toUpperCase());
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public void setThreadNum(String threadNum) {
        this.threadNum = Integer.valueOf(threadNum);
        if (this.threadNum <= 0 || this.threadNum > 2048) {
            throw new ConfigException("Invalid threadNum: " + threadNum);
        }
    }

    public void setFileSize(String fileSize) {
        this.fileSize = Integer.valueOf(fileSize);
        if (this.fileSize <= 0 || this.fileSize > (1024 * 1024 * 4)) {
            throw new ConfigException("Invalid fileSize: " + fileSize);
        }
    }

    public void setFileNum(String fileNum) {
        this.fileNum = Integer.valueOf(fileNum);
        if (this.fileNum <= 0 || this.fileNum > 100000) {
            throw new ConfigException("Invalid fileNum: " + fileNum);
        }
    }

    public String getDriverUrl() {
        return driverUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public ApiType getApiType() {
        return apiType;
    }

    public ApiMethod getApiMethod() {
        return apiMethod;
    }

    public String getWorkspace() {
        return workspace;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getFileNum() {
        return fileNum;
    }

    public String getFileDownloadPath() {
        return fileDownloadPath;
    }

    public void setFileDownloadPath(String fileDownloadPath) {
        this.fileDownloadPath = fileDownloadPath;
    }

    public String getRestUrl() {
        return restUrl;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }


    public boolean isReadFromDisk() {
        return readFromDisk;
    }

    public void setReadFromDisk(boolean readFromDisk) {
        this.readFromDisk = readFromDisk;
    }

    public String getFileReadPath() {
        return fileReadPath;
    }

    public void setFileReadPath(String fileReadPath) {
        this.fileReadPath = fileReadPath;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Config{");
        builder.append("driverUrl: ").append(driverUrl);
        builder.append(", restUrl: ").append(restUrl);
        builder.append(", apiType: ").append(apiType);
        builder.append(", apiMethod: ").append(apiMethod);
        builder.append(", workspace: ").append(workspace);
        builder.append(", threadNum: ").append(threadNum);
        builder.append(", readFromDisk: ").append(readFromDisk);
        builder.append(", fileSize: ").append(fileSize);
        builder.append(", fileNum: ").append(fileNum);
        builder.append(", fileDownloadPath: ").append(fileDownloadPath);
        builder.append(", fileReadPath: ").append(fileReadPath);

        builder.append("}");
        return builder.toString();
    }
}
