package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine;

/**
 * Super class of all location.
 */
public abstract class ScmLocation {
    private String siteName;

    /**
     * Create a data location with specified site name.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if argument is invalid.
     */
    public ScmLocation(String siteName) throws ScmInvalidArgumentException {
        checkValueNotNull(siteName, "siteName");
        this.siteName = siteName;
    }

    /**
     * Create a data location with a bson object.
     *
     * @param obj
     *            a bson containing information about data location.
     * @throws ScmInvalidArgumentException
     *             if argument is invalid.
     */
    public ScmLocation(BSONObject obj) throws ScmInvalidArgumentException {
        checkValueNotNull(obj, "obj");
        this.siteName = (String) obj.get(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME);
        if (siteName == null) {
            throw new ScmInvalidArgumentException(
                    "missing field:fieldName=" + CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME
                    + ", obj=" + obj.toString());
        }
    }

    /**
     * Gets the site name of the location.
     *
     * @return site name.
     */
    public String getSiteName() {
        return siteName;
    }

    /**
     * Sets the site name of the location.
     *
     * @param siteName
     *            site name.
     * @throws ScmInvalidArgumentException
     *             if site name is invalid.
     */
    public void setSiteName(String siteName) throws ScmInvalidArgumentException {
        checkValueNotNull(siteName, "siteName");
        this.siteName = siteName;
    }

    /**
     * Gets the Bson of the location.
     *
     * @return bson.
     */
    public BSONObject getBSONObject() {
        return new BasicBSONObject(CommonDefine.RestArg.WORKSPACE_LOCATION_SITE_NAME, siteName);
    }

    /**
     * Gets the datasource type of the location.
     *
     * @return type.
     */
    public abstract DatasourceType getType();

    void checkValueNotNull(Object v, String argName) throws ScmInvalidArgumentException {
        if (v == null) {
            throw new ScmInvalidArgumentException("invlid arg:" + argName + " is null");
        }
    }

    @Override
    public String toString() {
        return getBSONObject().toString();
    }

}
