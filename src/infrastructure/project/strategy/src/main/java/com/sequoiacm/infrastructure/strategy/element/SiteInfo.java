package com.sequoiacm.infrastructure.strategy.element;

public class SiteInfo {

    private int id;
    private int flag;
    
    public SiteInfo() {
    }
    
    public SiteInfo(int id, int flag) {
        this.id = id;
        this.flag = flag;
    }
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getFlag() {
        return flag;
    }
    public void setFlag(int flag) {
        this.flag = flag;
    }
    
    
}
