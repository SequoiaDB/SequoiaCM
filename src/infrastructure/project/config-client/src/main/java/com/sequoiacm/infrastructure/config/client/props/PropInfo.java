package com.sequoiacm.infrastructure.config.client.props;

public class PropInfo {
    private PropCheckRule checkRule;
    private boolean isRefreshable;

    public PropInfo(PropCheckRule checkRule, boolean isRefreshable) {
        this.checkRule = checkRule;
        this.isRefreshable = isRefreshable;
    }

    public PropCheckRule getCheckRule() {
        return checkRule;
    }

    public boolean isRefreshable() {
        return isRefreshable;
    }
}
