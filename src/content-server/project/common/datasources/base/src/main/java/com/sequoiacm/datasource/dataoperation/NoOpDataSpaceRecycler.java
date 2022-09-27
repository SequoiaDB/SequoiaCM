package com.sequoiacm.datasource.dataoperation;

public class NoOpDataSpaceRecycler implements ScmDataSpaceRecycler {

    @Override
    public ScmSpaceRecyclingInfo recycle(long maxExecTime, ScmSpaceRecyclingCallback callback) {
        return new ScmSpaceRecyclingInfo();
    }

    @Override
    public int getRecyclableTableCount() {
        return 0;
    }
}
