
package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;
import org.springframework.util.Assert;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;

public class ClientLocationOutline {
    private String siteName;
    private BSONObject others;
    private Integer id;

    public ClientLocationOutline(BSONObject clientLocation) throws ScmInvalidArgumentException {
        siteName = (String) clientLocation.get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        if (siteName == null) {
            throw new ScmInvalidArgumentException("missing site name:" + clientLocation);
        }
        clientLocation.removeField(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        others = clientLocation;
    }

    public String getSiteName() {
        return siteName;
    }

    public void addSiteId(int id) {
        this.id = id;
    }

    public BSONObject toCompleteBSON() {
        Assert.notNull(id, "add siteId first!");
        others.put(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID, id);
        return others;
    }

    @Override
    public String toString() {
        return "LocationOutline [siteName=" + siteName + ", others=" + others + ", id=" + id + "]";
    }

}