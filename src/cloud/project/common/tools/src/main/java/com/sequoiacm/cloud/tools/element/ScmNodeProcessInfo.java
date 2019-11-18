package com.sequoiacm.cloud.tools.element;

public class ScmNodeProcessInfo {
    private int pid;
    private String conf;
    private ScmNodeType type;

    public ScmNodeProcessInfo(int pid, String conf, ScmNodeType type) {
        super();
        this.pid = pid;
        this.conf = conf;
        this.type = type;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public ScmNodeType getType() {
        return type;
    }

    public void setType(ScmNodeType type) {
        this.type = type;
    }

}
