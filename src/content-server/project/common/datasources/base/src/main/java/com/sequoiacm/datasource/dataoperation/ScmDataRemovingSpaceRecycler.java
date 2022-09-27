package com.sequoiacm.datasource.dataoperation;

import org.bson.BSONObject;

public interface ScmDataRemovingSpaceRecycler {

    void notifyFileDataRemoving(BSONObject fileInfo);

    void notifyComplete();

    ScmSpaceRecyclingInfo getRecyclingInfo();

}
