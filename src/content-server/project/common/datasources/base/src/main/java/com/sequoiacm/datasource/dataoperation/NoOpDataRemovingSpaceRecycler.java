package com.sequoiacm.datasource.dataoperation;

import org.bson.BSONObject;

public class NoOpDataRemovingSpaceRecycler implements ScmDataRemovingSpaceRecycler {
    @Override
    public void notifyFileDataRemoving(BSONObject fileInfo) {

    }

    @Override
    public void notifyComplete() {

    }

    @Override
    public ScmSpaceRecyclingInfo getRecyclingInfo() {
        return new ScmSpaceRecyclingInfo();
    }
}
