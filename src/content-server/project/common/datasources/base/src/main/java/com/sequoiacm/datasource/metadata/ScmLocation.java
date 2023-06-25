package com.sequoiacm.datasource.metadata;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

import java.util.Date;

public abstract class ScmLocation {
    private int siteId;
    private String siteName;
    private BSONObject record;

    public abstract String getType();

    public ScmLocation(BSONObject record, String siteName) throws ScmDatasourceException {
        this.record = record;
        try {
            siteId = (int) record.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
            this.siteName = siteName;
        }
        catch (Exception e) {
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "get site id failed:fieldName=" + FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID,
                    e);
        }
    }

    protected ScmShardingType getShardingType(String shardingType) throws ScmDatasourceException {
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
        else if (shardingType.equals(ScmShardingType.DAY.getName())) {
            return ScmShardingType.DAY;
        }
        else {
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "unrecognized shardingType:type=" + shardingType);
        }
    }

    protected String getShardingStr(ScmShardingType type, Date createDate, String timezone) {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case YEAR:
                sb.append(CommonHelper.getCurrentYear(createDate, timezone));
                break;

            case MONTH:
                sb.append(CommonHelper.getCurrentYearMonth(createDate, timezone));
                break;

            case QUARTER:
                sb.append(CommonHelper.getCurrentYear(createDate, timezone));
                String month = CommonHelper.getCurrentMonth(createDate, timezone);
                sb.append(CommonHelper.getQuarter(month));
                break;

            case DAY:
                sb.append(CommonHelper.getCurrentDay(createDate, timezone));
                break;

            default:
                // default do nothing
                break;
        }

        return sb.toString();
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public BSONObject asBSON(){ return BsonUtils.deepCopyRecordBSON(record);}

    @Override
    public boolean equals(Object right) {
        if (right == this) {
            return true;
        }

        if (!(right instanceof ScmLocation)) {
            return false;
        }

        ScmLocation r = (ScmLocation) right;
        return siteId == r.siteId;
    }
}
