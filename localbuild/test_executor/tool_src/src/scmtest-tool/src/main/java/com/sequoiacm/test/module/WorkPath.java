package com.sequoiacm.test.module;

public class WorkPath {

    private String basePath;
    private String jarPath;
    private String tmpPath;
    private String dataPath;
    private String testProgressPath;
    private String consoleOutPath;
    private String testOutputPath;

    public WorkPath(String basePath, String pathSeparator) {
        basePath += pathSeparator;
        this.basePath = basePath;
        this.jarPath = basePath + "jars" + pathSeparator;
        this.tmpPath = basePath + "tmp" + pathSeparator;
        this.testProgressPath = tmpPath + "test-progress.json";
        this.dataPath = tmpPath + "data" + pathSeparator;
        this.consoleOutPath = basePath + "console-out" + pathSeparator;
        this.testOutputPath = basePath + "test-output" + pathSeparator;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getTmpPath() {
        return tmpPath;
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getTestProgressPath() {
        return testProgressPath;
    }

    public String getConsoleOutPath() {
        return consoleOutPath;
    }

    public String getTestOutputPath() {
        return testOutputPath;
    }
}
