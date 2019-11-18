package com.sequoiacm.infrastructure.strategy.element;

public class StrategyInfo {
    
    private int sourceSiteId;
    private int targetSiteId;
    private String connectivity;
    
    public StrategyInfo(int sourceSiteId, int targetSiteId, String connectivity) {
        super();
        this.sourceSiteId = sourceSiteId;
        this.targetSiteId = targetSiteId;
        this.connectivity = connectivity;
    }

    public StrategyInfo() {
    }
    
    public int getSourceSiteId() {
        return sourceSiteId;
    }
    public void setSourceSiteId(int sourceSiteId) {
        this.sourceSiteId = sourceSiteId;
    }
    public int getTargetSiteId() {
        return targetSiteId;
    }
    public void setTargetSiteId(int targetSiteId) {
        this.targetSiteId = targetSiteId;
    }
    public String getConnectivity() {
        return connectivity;
    }
    public void setConnectivity(String connectivity) {
        this.connectivity = connectivity;
    }
    
    
}
