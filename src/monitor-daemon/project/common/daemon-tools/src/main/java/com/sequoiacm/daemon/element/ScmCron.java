package com.sequoiacm.daemon.element;

public class ScmCron {
    private String user;
    private String linuxCron;
    private int period;

    public ScmCron(String user, String linuxCron, int period) {
        this.user = user;
        this.linuxCron = linuxCron;
        this.period = period;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getLinuxCron() {
        return linuxCron;
    }

    public void setLinuxCron(String linuxCron) {
        this.linuxCron = linuxCron;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
