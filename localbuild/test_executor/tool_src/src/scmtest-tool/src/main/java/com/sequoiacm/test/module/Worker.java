package com.sequoiacm.test.module;

public class Worker {

    private String name;
    private HostInfo hostInfo;
    private WorkPath workPath;

    public Worker(HostInfo hostInfo, int serial, String basePath, String pathSeparator) {
        this.hostInfo = hostInfo;

        String suffixStr = "";
        if (serial > 1) {
            suffixStr = suffixStr + "-" + serial;
        }
        this.name = hostInfo.getHostname() + suffixStr;
        this.workPath = new WorkPath(basePath + suffixStr, pathSeparator);
    }



    public boolean isLocalWorker() {
        return hostInfo.isLocalHost();
    }

    public String getName() {
        return name;
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

    public WorkPath getWorkPath() {
        return workPath;
    }
}
