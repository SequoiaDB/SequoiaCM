package com.sequoiacm.metasource.sequoiadb.config;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmMonthRange;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiadb.exception.SDBError;

public class SdbMetaSourceLocation implements MetaSourceLocation {

    private int siteId;
    private String domain;
    private ScmShardingType clShardingType = ScmShardingType.YEAR;
    private BSONObject clOptions;

    protected ScmShardingType getShardingType(String shardingType) throws SdbMetasourceException {
        if (shardingType.equals(ScmShardingType.YEAR.getName())) {
            return ScmShardingType.YEAR;
        }
        else if (shardingType.equals(ScmShardingType.MONTH.getName())) {
            return ScmShardingType.MONTH;
        }
        else if (shardingType.equals(ScmShardingType.NONE.getName())) {
            return ScmShardingType.NONE;
        }
        else if (shardingType.equals(ScmShardingType.QUARTER.getName())) {
            return ScmShardingType.QUARTER;
        }
        else {
            throw new SdbMetasourceException(SDBError.SDB_INVALIDARG.getErrorCode(),
                    "unreconigzed shardingType:type=" + shardingType);
        }
    }

    @Override
    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public SdbMetaSourceLocation(BSONObject metaLocation, BSONObject metaShardingType)
            throws SdbMetasourceException {
        try {
            siteId = (int) metaLocation.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
            domain = (String) metaLocation.get(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
            if (domain == null) {
                throw new SdbMetasourceException(ScmError.INVALID_ARGUMENT.getErrorCode(),
                        "domain not exists:" + metaLocation);
            }
            Object tmp = metaLocation.get(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE);
            if (tmp == null && metaShardingType != null) {
                tmp = metaShardingType.get(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE);
            }

            if (null != tmp) {
                if (tmp.equals("")) {
                    clShardingType = ScmShardingType.YEAR;
                }
                else {
                    clShardingType = getShardingType((String) tmp);
                }
            }

            if (ScmShardingType.YEAR != clShardingType && ScmShardingType.MONTH != clShardingType
                    && ScmShardingType.QUARTER != clShardingType) {
                throw new SdbMetasourceException(SDBError.SDB_INVALIDARG.getErrorCode(),
                        "meta's shardingType can't be supportted:type=" + tmp);
            }

            parseMetaOptions(metaLocation);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_INVALIDARG.getErrorCode(),
                    "get site id failed:fieldName=" + FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID,
                    e);
        }
    }

    private void parseMetaOptions(BSONObject metaLocation) {
        BSONObject metaOptions = (BSONObject) metaLocation
                .get(FieldName.FIELD_CLWORKSPACE_META_OPTIONS);
        if (metaOptions == null) {
            clOptions = new BasicBSONObject();
        }
        else {
            clOptions = (BSONObject) metaOptions.get(FieldName.FIELD_CLWORKSPACE_META_CL);
            if (clOptions == null) {
                clOptions = new BasicBSONObject();
            }
        }
    }

    public SdbClFileInfo getClFileInfo(String mainClName, Date createDate) {
        String shardingStr = CommonHelper.getShardingStr(clShardingType, createDate);

        StringBuilder sb = new StringBuilder();
        sb.append(mainClName);
        sb.append("_");
        sb.append(shardingStr);

        StringBuilder sbHistory = new StringBuilder();
        sbHistory.append(mainClName + "_HISTORY");
        sbHistory.append("_");
        sbHistory.append(shardingStr);

        SdbClFileInfo metaInfo = new SdbClFileInfo(sb.toString(), sbHistory.toString(), "", "",
                clOptions);
        ScmMonthRange range = CommonHelper.getMonthRange(clShardingType, createDate);
        metaInfo.setLowMonth(range.getLowBound());
        metaInfo.setUpperMonth(range.getUpBound());
        return metaInfo;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public String getType() {
        // TODO:
        return "sequoiadb";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(getSiteId()).append(",");
        sb.append("domain=").append(domain);

        return sb.toString();
    }

    @Override
    public boolean equals(Object right) {
        if (right == this) {
            return true;
        }

        if (!(right instanceof SdbMetaSourceLocation)) {
            return false;
        }

        if (!super.equals(right)) {
            return false;
        }

        SdbMetaSourceLocation r = (SdbMetaSourceLocation) right;
        return siteId == r.siteId && domain.equals(r.domain);
    }
}
