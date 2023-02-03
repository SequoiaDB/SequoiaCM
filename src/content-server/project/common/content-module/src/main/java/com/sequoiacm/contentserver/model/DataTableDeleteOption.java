package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;

public class DataTableDeleteOption {
    BSONObject wsLocalSiteLocation;

    public DataTableDeleteOption(BSONObject wsLocalSiteLocation) {
        this.wsLocalSiteLocation = wsLocalSiteLocation;
    }

    public BSONObject getWsLocalSiteLocation() {
        return wsLocalSiteLocation;
    }

}
