package com.sequoiacm.tools.element;

import java.util.Date;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmFiledDefine;
import com.sequoiacm.tools.common.SdbHelper;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmWorkspaceInfo {

    private String name;
    private int id;
    // private int metaSiteId;
    // private String metaDomain;

    private String dataClShardType = ScmFiledDefine.WORKSPACE_SHARDING_MONTH_STR;
    private String dataCsSharType = ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR;
    private String metaShardType;

    private BasicBSONList dataLocationBSON = new BasicBSONList();
    private BSONObject metaLocationBSON = new BasicBSONObject();

    private BSONObject dataShardingTypeBSON;
    private BSONObject dataOptionBSON;

    // private Map<Integer, String> dataSite2Domain = new HashMap<>();

    public ScmWorkspaceInfo() {
        this.id = -1;
    }

    public ScmWorkspaceInfo(BSONObject obj) throws ScmToolsException {
        String name = (String) obj.get(FieldName.FIELD_CLWORKSPACE_NAME);
        checkNotNull(name, obj, FieldName.FIELD_CLWORKSPACE_NAME);

        BasicBSONList dataLocationList = (BasicBSONList) obj
                .get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        checkNotNull(dataLocationList, obj, FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        dataLocationBSON = dataLocationList;

        Integer id = (Integer) obj.get(FieldName.FIELD_CLWORKSPACE_ID);
        checkNotNull(id, obj, FieldName.FIELD_CLWORKSPACE_ID);

        BSONObject meta_location = (BSONObject) obj.get(FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        checkNotNull(meta_location, obj, FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        metaLocationBSON = meta_location;

        Integer metaSiteId = (Integer) meta_location
                .get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
        checkNotNull(metaSiteId, obj, FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);

        String metaDomain = (String) meta_location.get(FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
        checkNotNull(metaDomain, obj, FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);

        BSONObject dataShardingType = (BSONObject) obj
                .get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (dataShardingType != null) {
            dataShardingTypeBSON = dataShardingType;
            String csShardingType = (String) dataShardingType
                    .get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
            if (csShardingType != null && !csShardingType.equals("")) {
                this.dataCsSharType = csShardingType;
            }
            String clShardingType = (String) dataShardingType
                    .get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            if (clShardingType != null && !clShardingType.equals("")) {
                this.dataClShardType = clShardingType;
            }
        }
        else {
            dataShardingTypeBSON = null;
        }

        String metaShardingType = (String) obj.get(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE);
        if (metaShardingType != null && !metaShardingType.equals("")) {
            this.metaShardType = metaShardingType;
        }

        BSONObject dataOptions = (BSONObject) obj.get(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS);
        if (dataOptions != null) {
            dataOptionBSON = dataOptions;
        }
        else {
            dataOptionBSON = null;
        }

        this.name = name;
        this.id = id;
    }

    public BSONObject getDataLocation(int siteId) throws ScmToolsException {
        for (Object obj : dataLocationBSON) {
            BSONObject tmp = (BSONObject) obj;
            int id = (int) SdbHelper.getValueWithCheck(tmp,
                    FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
            if (id == siteId) {
                return tmp;
            }
        }
        throw new ScmToolsException("workspace dose not contain this site:ws=" + name + ",siteId="
                + siteId, ScmExitCode.SYSTEM_ERROR);
    }

    public String getClShardingType(int siteId) throws ScmToolsException {
        BSONObject dataLocation = getDataLocation(siteId);
        BSONObject sharding = (BSONObject) dataLocation
                .get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (sharding != null) {
            String clSharing = (String) sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CL);
            if (clSharing != null) {
                return clSharing;
            }
        }
        return dataClShardType;
    }

    public String getCsShardingType(int siteId) throws ScmToolsException {
        BSONObject dataLocation = getDataLocation(siteId);
        BSONObject sharding = (BSONObject) dataLocation
                .get(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE);
        if (sharding != null) {
            String clSharing = (String) sharding.get(FieldName.FIELD_CLWORKSPACE_DATA_CS);
            if (clSharing != null) {
                return clSharing;
            }
        }
        return dataCsSharType;
    }

    public String getName() {
        return name;
    }

    public String getMetaShardType() {
        return metaShardType;
    }

    public void setMetaShardType(String metaShardType) {
        this.metaShardType = metaShardType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMetaLocationSiteId() throws ScmToolsException {
        return (int) SdbHelper.getValueWithCheck(metaLocationBSON,
                FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
    }

    public String getMetaLocationSiteDomain() throws ScmToolsException {
        return (String) SdbHelper.getValueWithCheck(metaLocationBSON,
                FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
    }

    public MetaCLNameInfo getMetaCLName(Date createDate) {
        String metaShardingType = (String) metaLocationBSON
                .get(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE);
        if (metaShardingType == null) {
            if (this.metaShardType == null) {
                metaShardingType = ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR;
            }
            else {
                metaShardingType = this.metaShardType;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(SdbHelper.CL_WS_FILE);

        StringBuilder sbHistory = new StringBuilder();
        sbHistory.append(SdbHelper.CL_WS_FILE_HISTORY);

        String lowYearMonth = "";
        String upperYearMonth = "";

        if (metaShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            String year = ScmCommon.DateUtil.getCurrentYear(createDate);
            sb.append("_");
            sb.append(year);

            sbHistory.append("_");
            sbHistory.append(year);

            lowYearMonth = year + ScmCommon.DateUtil.MONTH1;

            String nextYear = ScmCommon.DateUtil.getNextYear(createDate);
            upperYearMonth = nextYear + ScmCommon.DateUtil.MONTH1;
        }
        else if (metaShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_MONTH_STR)) {
            String yearMonth = ScmCommon.DateUtil.getCurrentYearMonth(createDate);
            sb.append("_");
            sb.append(yearMonth);

            sbHistory.append("_");
            sbHistory.append(yearMonth);

            lowYearMonth = yearMonth;
            upperYearMonth = ScmCommon.DateUtil.getNextYearMonth(createDate);
        }
        else {
            // metaShardingType == SHARDING_TYPE_QUARTER
            String quarter = ScmCommon.DateUtil.getQuarter(ScmCommon.DateUtil
                    .getCurrentMonth(createDate));
            String year = ScmCommon.DateUtil.getCurrentYear(createDate);

            sb.append("_");
            sb.append(year);
            sb.append(quarter);

            sbHistory.append("_");
            sbHistory.append(year);
            sbHistory.append(quarter);

            if (quarter.equals(ScmCommon.DateUtil.QUARTER1)) {
                lowYearMonth = year + ScmCommon.DateUtil.MONTH1;
                upperYearMonth = year + ScmCommon.DateUtil.MONTH4;
            }
            else if (quarter.equals(ScmCommon.DateUtil.QUARTER2)) {
                lowYearMonth = year + ScmCommon.DateUtil.MONTH4;
                upperYearMonth = year + ScmCommon.DateUtil.MONTH7;
            }
            else if (quarter.equals(ScmCommon.DateUtil.QUARTER3)) {
                lowYearMonth = year + ScmCommon.DateUtil.MONTH7;
                upperYearMonth = year + ScmCommon.DateUtil.MONTH10;
            }
            else {
                // QUARTER4
                lowYearMonth = year + ScmCommon.DateUtil.MONTH10;

                String nextYear = ScmCommon.DateUtil.getNextYear(createDate);
                upperYearMonth = nextYear + ScmCommon.DateUtil.MONTH1;
            }
        }

        return new MetaCLNameInfo(sb.toString(), sbHistory.toString(), lowYearMonth, upperYearMonth);
    }

    public BSONObject toBSON() {
        BSONObject res = new BasicBSONObject();
        res.put(FieldName.FIELD_CLWORKSPACE_ID, id);
        res.put(FieldName.FIELD_CLWORKSPACE_NAME, name);

        // metalocation
        res.put(FieldName.FIELD_CLWORKSPACE_META_LOCATION, metaLocationBSON);

        res.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, dataLocationBSON);
        if (metaShardType != null) {
            res.put(FieldName.FIELD_CLWORKSPACE_META_SHARDING_TYPE, metaShardType);
        }

        if (dataShardingTypeBSON != null) {
            res.put(FieldName.FIELD_CLWORKSPACE_DATA_SHARDING_TYPE, dataShardingTypeBSON);
        }
        if (dataOptionBSON != null) {
            res.put(FieldName.FIELD_CLWORKSPACE_DATA_OPTIONS, dataOptionBSON);
        }

        return res;
    }

    private void checkNotNull(Object obj, BSONObject bsonobj, String field)
            throws ScmToolsException {
        if (obj == null) {
            throw new ScmToolsException("this worksapce collection record that without field( "
                    + field + "):" + bsonobj.toString(), ScmExitCode.SCM_META_RECORD_ERROR);
        }
    }

    public void setDataShardingTypeBSON(BSONObject dataShardingTypeBSON) {
        this.dataShardingTypeBSON = dataShardingTypeBSON;
    }

    public void setDataOptionBSON(BSONObject dataOptionBSON) {
        this.dataOptionBSON = dataOptionBSON;
    }

    public String getDataCsName(Date createDate,int siteId) throws ScmToolsException {
        String dataCsShard = getCsShardingType(siteId);
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(SdbHelper.CS_LOB_WS_TAIL);

        if (dataCsShard.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            sb.append("_");
            sb.append(ScmCommon.DateUtil.getCurrentYear(createDate));
        }
        else if (dataCsShard.equals(ScmFiledDefine.WORKSPACE_SHARDING_MONTH_STR)) {
            sb.append("_");
            sb.append(ScmCommon.DateUtil.getCurrentYearMonth(createDate));
        }
        else if (dataCsShard.equals(ScmFiledDefine.WORKSPACE_SHARDING_QUARTER_STR)) {
            sb.append("_");
            sb.append(ScmCommon.DateUtil.getCurrentYear(createDate));
            String month = ScmCommon.DateUtil.getCurrentMonth(createDate);
            sb.append(getQuarter(month));
        }
        else {
            // SHARDING_TYPE_NONE append nothing
        }

        return sb.toString();
    }

    public String getDataClName(Date createDate,int siteId) throws ScmToolsException {
        String clSharding = getClShardingType(siteId);
        StringBuilder sb = new StringBuilder();
        sb.append(SdbHelper.CL_WS_LOB);

        if (clSharding.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            sb.append("_");
            sb.append(ScmCommon.DateUtil.getCurrentYear(createDate));
        }
        else if (clSharding.equals(ScmFiledDefine.WORKSPACE_SHARDING_MONTH_STR)) {
            sb.append("_");
            sb.append(ScmCommon.DateUtil.getCurrentYearMonth(createDate));
        }
        else if (clSharding.equals(ScmFiledDefine.WORKSPACE_SHARDING_QUARTER_STR)) {
            sb.append("_");
            sb.append(ScmCommon.DateUtil.getCurrentYear(createDate));
            String month = ScmCommon.DateUtil.getCurrentMonth(createDate);
            sb.append(getQuarter(month));
        }
        else {
            // SHARDING_TYPE_NONE append nothing
        }

        return sb.toString();
    }

    private String getQuarter(String month) {
        StringBuilder sb = new StringBuilder();
        if (month.compareTo(ScmCommon.DateUtil.MONTH6) <= 0) {
            if (month.compareTo(ScmCommon.DateUtil.MONTH3) <= 0) {
                sb.append(ScmCommon.DateUtil.QUARTER1);
            }
            else {
                sb.append(ScmCommon.DateUtil.QUARTER2);
            }
        }
        else {
            if (month.compareTo(ScmCommon.DateUtil.MONTH9) <= 0) {
                sb.append(ScmCommon.DateUtil.QUARTER3);
            }
            else {
                sb.append(ScmCommon.DateUtil.QUARTER4);
            }
        }

        return sb.toString();
    }

    public String getSmaller(String dataCsSharType, String dataClShardType) {
        if (dataCsSharType.equals(ScmFiledDefine.WORKSPACE_SHARDING_NONE_STR)) {
            return dataClShardType;
        }
        if (dataCsSharType.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            return dataClShardType;
        }
        if (dataClShardType.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            return dataCsSharType;
        }
        if (dataCsSharType.equals(ScmFiledDefine.WORKSPACE_SHARDING_QUARTER_STR)) {
            return dataClShardType;
        }
        return dataCsSharType;

    }

    public BasicBSONList getDataLocationBSON() {
        return dataLocationBSON;
    }

    public void setDataLocationBSON(BasicBSONList dataLocation) {
        this.dataLocationBSON = dataLocation;
    }

    public BSONObject getMetaLocationBSON() {
        return metaLocationBSON;
    }

    public void setMetaLocationBSON(BSONObject obj) {
        this.metaLocationBSON = obj;
    }

    public BSONObject getDataShardingTypeBSON() {
        return dataShardingTypeBSON;
    }

    public BSONObject getDataOptionBSON() {
        return dataOptionBSON;
    }

}
