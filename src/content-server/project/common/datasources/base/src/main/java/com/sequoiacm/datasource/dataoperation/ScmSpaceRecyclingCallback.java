package com.sequoiacm.datasource.dataoperation;

public interface ScmSpaceRecyclingCallback {

    boolean shouldContinue();

    ScmSpaceRecyclingCallback DEFAULT_CALLBACK = new ScmSpaceRecyclingCallback() {
        @Override
        public boolean shouldContinue() {
            return true;
        }
    };

}
