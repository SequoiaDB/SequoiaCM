package com.sequoiacm.s3.remote;

import java.util.List;

public class ScmWsInfo {
    private String name;
    private int id;
    private List<Integer> sites;
    private Long createTime;

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getSites() {
        return sites;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSites(List<Integer> sites) {
        this.sites = sites;
    }

}
