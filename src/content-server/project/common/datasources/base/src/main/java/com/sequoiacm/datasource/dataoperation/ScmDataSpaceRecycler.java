package com.sequoiacm.datasource.dataoperation;

public interface ScmDataSpaceRecycler {

    ScmSpaceRecyclingInfo recycle(long maxExecTime, ScmSpaceRecyclingCallback callback);

    int getRecyclableTableCount();

}
