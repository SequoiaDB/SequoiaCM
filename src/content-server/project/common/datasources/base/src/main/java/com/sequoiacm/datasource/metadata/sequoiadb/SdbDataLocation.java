package com.sequoiacm.datasource.metadata.sequoiadb;

import java.util.Calendar;
import java.util.Date;

import com.sequoiacm.common.CommonHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;

public class SdbDataLocation extends SdbLocation {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataLocation.class);

    private ScmShardingType csShardingType = ScmShardingType.YEAR;
    private ScmShardingType clShardingType = ScmShardingType.MONTH;
    private BSONObject dataCSOptions;
    private BSONObject dataCLOptions;

    public SdbDataLocation(BSONObject dataLocation, String siteName) throws ScmDatasourceException {
        super(dataLocation, siteName);

        Object tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (null != tmp) {
            BSONObject sharding = (BSONObject) tmp;
            tmp = sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
            if (null != tmp) {
                if (tmp.equals("")) {
                    csShardingType = ScmShardingType.YEAR;
                }
                else {
                    csShardingType = getShardingType((String) tmp);
                }
            }

            tmp = sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            if (null != tmp) {
                if (tmp.equals("")) {
                    clShardingType = ScmShardingType.MONTH;
                }
                else {
                    clShardingType = getShardingType((String) tmp);
                }
            }
        }

        if (ScmShardingType.NONE == clShardingType) {
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "data collection's shardingType can't be supportted:type=" + tmp);
        }

        tmp = dataLocation.get(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS);
        parseDataOptions((BSONObject) tmp);
    }

    public BSONObject getDataCSOptions() {
        return dataCSOptions;
    }

    public BSONObject getDataCLOptions() {
        return dataCLOptions;
    }

    private void parseDataOptions(BSONObject dataOptions) throws ScmDatasourceException {
        if (null == dataOptions) {
            dataCSOptions = new BasicBSONObject();
            dataCLOptions = new BasicBSONObject();
            return;
        }

        Object tmp = null;
        try {
            tmp = dataOptions.get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
            if (null == tmp) {
                dataCSOptions = new BasicBSONObject();
            }
            else {
                dataCSOptions = (BSONObject) tmp;
            }

            tmp = dataOptions.get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            if (null == tmp) {
                dataCLOptions = new BasicBSONObject();
            }
            else {
                dataCLOptions = (BSONObject) tmp;
            }
        }
        catch (Exception e) {
            throw new ScmDatasourceException(ScmError.INVALID_ARGUMENT,
                    "dataOptions=" + dataOptions.toString(), e);
        }
    }

    public String getDataCsName(String wsName, Date createDate) {
        return getDataCsName(wsName, getShardingStr(csShardingType, createDate));
    }

    public String getDataCsName(String wsName, String shardingStr) {
        StringBuilder sb = new StringBuilder();
        sb.append(wsName);
        sb.append(SdbMetaDefine.CS_LOB_EXTRA);
        if (csShardingType != ScmShardingType.NONE) {
            sb.append("_");
            sb.append(shardingStr);
        }
        return sb.toString();
    }

    public ScmShardingType getCsShardingType() {
        return csShardingType;
    }

    public String getDataClName(Date createDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(SdbMetaDefine.CL_LOB);
        if (clShardingType != ScmShardingType.NONE) {
            sb.append("_");
            sb.append(getShardingStr(clShardingType, createDate));
        }

        return sb.toString();
    }

    public String getCsShardingStr(Date createDate) {
        return super.getShardingStr(csShardingType, createDate);
    }

    @Override
    public boolean equals(Object right) {
        if (right == this) {
            return true;
        }

        if (!(right instanceof SdbLocation)) {
            return false;
        }

        if (!super.equals(right)) {
            return false;
        }

        SdbDataLocation r = (SdbDataLocation) right;
        return csShardingType.equals(r.csShardingType) && clShardingType.equals(r.clShardingType)
                && dataCSOptions.equals(r.dataCSOptions) && dataCLOptions.equals(r.dataCLOptions);
    }

    public static Date getCsShardingBeginningTime(String csName, String wsName) {
        // ws_default_LOB --> null
        // ws_default_LOB_2022 --> 20220101
        // ws_default_LOB_202203 --> 20220301
        // ws_default_LOB_2022Q3 --> 20220901
        // ws_default_LOB_20220801 --> 20220801
        String shardingStr = csName
                .substring(wsName.length() + SdbMetaDefine.CS_LOB_EXTRA.length() + 1);
        if (shardingStr.length() == 0) {
            return null;
        }

        try {
            Calendar calendar = parseCSTime(shardingStr);
            return calendar.getTime();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(
                    "failed to parse collection space sharding time:csName=" + csName + ", wsName="
                            + wsName);
        }
    }

    private static Calendar createCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        return calendar;
    }

    private static Calendar parseCSTime(String shardingStr) {
        Calendar calendar = createCalendar();
        if (shardingStr.length() < 4) {
            throw new IllegalArgumentException("unrecognized shardingStr" + shardingStr);
        }

        calendar.set(Calendar.YEAR, Integer.parseInt(shardingStr.substring(0, 4)));
        calendar.set(Calendar.MONTH, calendar.getActualMinimum(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        if (shardingStr.length() >= 6) {
            if (shardingStr.substring(4, 5).equals("Q")) { // QUARTER
                int quarterStartMonth = CommonHelper
                        .getQuarterStartMonth(shardingStr.substring(4, 6));
                calendar.set(Calendar.MONTH, quarterStartMonth - 1);
            }
            else { // MONTH
                calendar.set(Calendar.MONTH, Integer.parseInt(shardingStr.substring(4, 6)) - 1);
                if (shardingStr.length() >= 8) {
                    calendar.set(Calendar.DAY_OF_MONTH,
                            Integer.parseInt(shardingStr.substring(6, 8)));
                }
            }
        }

        return calendar;
    }

}
