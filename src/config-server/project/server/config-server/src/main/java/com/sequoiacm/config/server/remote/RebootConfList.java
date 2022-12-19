package com.sequoiacm.config.server.remote;

import java.util.Set;

public class RebootConfList {
    private Set<String> conf;

    public RebootConfList(Set<String> conf) {
        this.conf = conf;
    }

    public RebootConfList() {
    }

    public Set<String> getConf() {
        return conf;
    }

    @Override
    public String toString() {
        return "RebootConfList{" + "conf=" + conf + '}';
    }
}
