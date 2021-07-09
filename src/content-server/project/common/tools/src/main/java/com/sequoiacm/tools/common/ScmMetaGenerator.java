package com.sequoiacm.tools.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.tools.element.MetaCLNameInfo;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmSimpleFileInfo;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.element.ScmWorkspaceInfo;
import com.sequoiacm.tools.element.TimeArgWrapper;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

public class ScmMetaGenerator {

    private Sequoiadb db;
    private DBCollection lobCL;
    private DBCollection metaCL;
    private CollectionSpace metaCS;
    private ScmMetaMgr mg;

    private final Logger logger = LoggerFactory.getLogger(ScmMetaGenerator.class);
    private DBCollection metaHistoryCL;
    private ScmWorkspaceInfo wsInfo;
    private final int INTERVAL = 10000;
    private int count;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    private SimpleDateFormat sdfHuman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String smallerDataShard;
    private TimeArgWrapper timeWrapper;
    private String csShardingType;
    private String clShardingType;
    private ScmSiteInfo rootSite;

    public ScmMetaGenerator(String wsName, String lobCsName, String lobClName, ScmSdbInfo mainSdb)
            throws ScmToolsException {
        db = SdbHelper.connectUrls(mainSdb.getSdbUrl(), mainSdb.getSdbUser(),
                mainSdb.getPlainSdbPasswd());
        mg = new ScmMetaMgr(db);
        try {
            wsInfo = mg.getWorkspaceInfoByName(wsName);
            rootSite = mg.getMainSiteChecked();
            if (wsInfo == null) {
                logger.error("Workspace not exists:" + wsName);
                throw new ScmToolsException("Workspace not exists:" + wsName,
                        ScmExitCode.SCM_WORKSPACE_NOT_EXIST);
            }
            csShardingType = wsInfo.getCsShardingType(rootSite.getId());
            logger.info("root site data cs sharding type:" + csShardingType);
            clShardingType = wsInfo.getClShardingType(rootSite.getId());
            logger.info("root site data cl sharding type:" + clShardingType);
            smallerDataShard = wsInfo.getSmaller(csShardingType, clShardingType);

            CheckClFullName(lobCsName, lobClName);
            // if
            // (!wsInfo.getClShardingType().equals(timeWrapper.getShardingType())
            // && !wsInfo
            // .getClShardingType().equals(ScmFiledDefine.WORKSPACE_SHARDING_NONE_STR))
            // {
            // logger.error("Cl sharding type is " + wsInfo.getClShardingType()
            // + ",invalid time arg:" + timeWrapper.getTime());
            // throw new ScmToolsException("Cl sharding type is " +
            // wsInfo.getClShardingType()
            // + ",invalid time arg:" + timeWrapper.getTime(),
            // ScmExitCode.INVALID_ARG);
            // }
            CollectionSpace lobCS = SdbHelper.getCSWithCheck(db, lobCsName);
            lobCL = SdbHelper.getCLWithCheck(lobCS, lobClName);
            metaCS = SdbHelper.getCSWithCheck(db, wsInfo.getName() + SdbHelper.CS_META_WS_TAIL);
            metaCL = SdbHelper.getCLWithCheck(metaCS, SdbHelper.CL_WS_FILE);
            metaHistoryCL = SdbHelper.getCLWithCheck(metaCS, SdbHelper.CL_WS_FILE_HISTORY);

        }
        catch (ScmToolsException e) {
            logger.error("Failed to init ScmMetaGenerate", e);
            close();
            throw e;
        }
    }

    private void CheckClFullName(String lobCsName, String lobClName) throws ScmToolsException {
        if (csShardingType.equals(smallerDataShard)) {
            timeWrapper = getTimeFromCsName(lobCsName);
        }
        else {
            timeWrapper = getTimeFromCLName(lobClName);
        }
        String correctCsName = wsInfo.getDataCsName(timeWrapper.getLower(), rootSite.getId());
        String correctClName = wsInfo.getDataClName(timeWrapper.getLower(), rootSite.getId());
        if (!lobCsName.equals(correctCsName) || !lobClName.equals(correctClName)) {
            logger.error("Invalid collection full name:" + lobCsName + "." + lobClName + ",expect:"
                    + correctCsName + "." + correctClName);
            throw new ScmToolsException("Invalid collection full name:" + lobCsName + "."
                    + lobClName, ScmExitCode.INVALID_ARG);
        }

    }

    private TimeArgWrapper getTimeFromCLName(String lobClName) throws ScmToolsException {
        String regex;
        if (clShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            regex = "^LOB_(\\d{4})$";
        }
        else if (clShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_MONTH_STR)) {
            regex = "^LOB_\\d{4}(0[1-9]|1[0-2])$";
        }
        else if (clShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_QUARTER_STR)) {
            regex = "^LOB_(\\d{4})Q[1-4]$";
        }
        else {
            logger.error("unkonw cl sharding type:" + clShardingType);
            throw new ScmToolsException("unkonw cl sharding type:" + clShardingType,
                    ScmExitCode.SCM_META_RECORD_ERROR);
        }
        Pattern pattern = Pattern.compile(regex);
        if (pattern.matcher(lobClName).find()) {
            TimeArgWrapper clTimeWrapper = new TimeArgWrapper(lobClName.substring(4,
                    lobClName.length()));
            return clTimeWrapper;
        }
        else {
            throw new ScmToolsException("Invalid collection name:" + lobClName
                    + ",workspace cl sharding type is " + clShardingType, ScmExitCode.INVALID_ARG);
        }

    }

    private TimeArgWrapper getTimeFromCsName(String lobCsName) throws ScmToolsException {
        if (!lobCsName.startsWith(wsInfo.getName() + SdbHelper.CS_LOB_WS_TAIL)) {
            throw new ScmToolsException("Invalid collectionspace name:" + lobCsName
                    + ",cs name must start with:" + wsInfo.getName() + SdbHelper.CS_LOB_WS_TAIL,
                    ScmExitCode.INVALID_ARG);
        }
        String csNametail = lobCsName.replace(wsInfo.getName() + SdbHelper.CS_LOB_WS_TAIL, "");
        String regex;

        if (csShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_NONE_STR)) {
            if (!csNametail.equals("")) {
                throw new ScmToolsException("Invalid collectionspace name:" + lobCsName
                        + ",workspace cs sharding type is " + csShardingType,
                        ScmExitCode.INVALID_ARG);
            }
            return null;
        }
        else if (csShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            regex = "^_\\d{4}$";
        }
        else if (csShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_MONTH_STR)) {
            regex = "^_\\d{4}(0[1-9]|1[0-2])$";
        }
        else if (csShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_QUARTER_STR)) {
            regex = "^_\\d{4}Q[1-4]$";
        }
        else {
            logger.error("unkonw cs sharding type:" + csShardingType);
            throw new ScmToolsException("unkonw cl sharding type:" + csShardingType,
                    ScmExitCode.SCM_META_RECORD_ERROR);
        }
        Pattern pattern = Pattern.compile(regex);
        if (pattern.matcher(csNametail).find()) {
            TimeArgWrapper csTimeWrapper = new TimeArgWrapper(csNametail.substring(1,
                    csNametail.length()));
            return csTimeWrapper;
        }
        else {
            throw new ScmToolsException("Invalid collectionspace name:" + lobCsName
                    + ",workspace cs sharding type is " + csShardingType, ScmExitCode.INVALID_ARG);
        }

    }

    public void generate() throws ScmToolsException {
        int mainSiteId = mg.getMainSiteChecked().getId();
        String user = mg.getUser();
        DBCursor c = SdbHelper.listLobs(lobCL);
        count = 0;
        try {
            while (c.hasNext()) {
                BSONObject lob = c.getNext();
                Boolean available = (Boolean) SdbHelper.getValueWithCheck(lob,
                        ScmFiledDefine.LOB_AVAILABLE);
                ObjectId oid = (ObjectId) SdbHelper.getValueWithCheck(lob, ScmFiledDefine.LOB_OID);
                if (available == false) {
                    System.out.println("WARN:skip unavailable lob:" + oid.toString());
                    logger.warn("WARN:skip unavailable lob:" + oid.toString());
                    continue;
                }
                Long size = (Long) SdbHelper.getValueWithCheck(lob, ScmFiledDefine.LOB_SIZE);
                BSONTimestamp createTime = (BSONTimestamp) SdbHelper.getValueWithCheck(lob,
                        ScmFiledDefine.LOB_CREATE_TIME);
                Date lobCreateDate = null;
                if (!timeWrapper.isContain(createTime.getDate())) {
                    lobCreateDate = correctDate(timeWrapper.getLower(), createTime.getDate());
                    String oldDateStr = sdfHuman.format(createTime.getDate());
                    String newDateStr = sdfHuman.format(lobCreateDate);
                    System.out.println("WARN:correct out of bounds date,old date:" + oldDateStr
                            + ",new date:" + newDateStr + ",oid:" + oid.toString());
                    logger.warn("correct out of bounds date,old date:" + oldDateStr + ",new date:"
                            + newDateStr + ",oid:" + oid.toString());
                }
                else {
                    lobCreateDate = createTime.getDate();
                }

                long lobCreateMill = lobCreateDate.getTime() + createTime.getInc() / 1000;

                long metaCreateSec;
                try {
                    ScmIdParser idP = new ScmIdParser(oid.toString());
                    metaCreateSec = idP.getSecond();
                }
                catch (Exception e) {
                    logger.error("ERROR:failed to get second from id:" + oid.toString(), e);
                    System.out.println("failed to get second from id:" + oid.toString());
                    continue;
                }

                ScmSimpleFileInfo file = new ScmSimpleFileInfo(size, metaCreateSec, lobCreateMill,
                        oid.toString(), mainSiteId, user);
                insertFile(file, lobCreateDate);
                if (count % INTERVAL == 0 && count != 0) {
                    System.out.println("INFO:" + count + " meta has been generated");
                    logger.info(count + "  meta has been generated");
                }
            }
        }
        catch (BaseException e) {
            logger.error("list lob occur exception", e);
            throw new ScmToolsException("list lob occur exception:"
                    + SdbHelper.processSdbErrorMsg(e), ScmExitCode.SDB_QUERY_ERROR);
        }
    }

    private Date correctDate(Date lower, Date date) throws ScmToolsException {
        String lowerStr = sdf.format(lower);
        String dateStr = sdf.format(date);
        String retStr;

        if (smallerDataShard.equals(ScmFiledDefine.WORKSPACE_SHARDING_YEAR_STR)) {
            retStr = lowerStr.substring(0, 4) + dateStr.substring(4);
        }
        else {
            retStr = lowerStr.substring(0, 6) + dateStr.substring(6);
        }

        try {
            return sdf.parse(retStr);
        }
        catch (ParseException e) {
            logger.error("failed to correct date,parse '" + retStr + "' error", e);
            throw new ScmToolsException("failed to correct date,parse '" + retStr + "' error:"
                    + e.getMessage(), ScmExitCode.PARSE_ERROR);
        }
    }

    private void insertFile(ScmSimpleFileInfo file, Date createDate) throws ScmToolsException {
        try {
            metaCL.insert(file.get());
            count++;

        }
        catch (BaseException e) {
            if (e.getErrorCode() != SdbHelper.SdbErrorCode.SDB_CAT_NO_MATCH_CATALOG) {
                System.out.println("ERROR:Failed to insert file(" + file.getId() + ") to "
                        + metaCL.getFullName() + ",sdb error code:" + e.getErrorCode());
                logger.error(
                        "Failed to insert file(" + file.getId() + ") to" + metaCL.getFullName(), e);
                return;
            }
            createSubCL(createDate);
            createSubHistoryCL(createDate);
            insertFile(file, createDate);
        }
    }

    private void createSubHistoryCL(Date createDate) throws ScmToolsException {
        MetaCLNameInfo metaClName = wsInfo.getMetaCLName(createDate);
        BSONObject options = getFileCLOption(metaClName.getClHistoryName());
        System.out.println("INFO:creating collection:" + metaCS.getName() + "."
                + metaClName.getClHistoryName());
        logger.info("creating cl:cl=" + metaCS.getName() + "." + metaClName.getClHistoryName()
        + ",options=" + options.toString());
        DBCollection cl = SdbHelper.createCL(metaCS, metaClName.getClHistoryName(), options);

        BSONObject indexDef = new BasicBSONObject();
        indexDef.put(FieldName.FIELD_CLFILE_ID, 1);
        indexDef.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, 1);
        indexDef.put(FieldName.FIELD_CLFILE_MINOR_VERSION, 1);
        SdbHelper.createIdx(cl, "idx_" + cl.getName() + "_" + FieldName.FIELD_CLFILE_ID
                + "_version", indexDef.toString(), true, false);

        indexDef = new BasicBSONObject();
        indexDef.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, 1);
        SdbHelper.createIdx(cl, "idx_" + cl.getName() + "_" + FieldName.FIELD_CLFILE_FILE_DATA_ID,
                indexDef.toString(), false, false);

        BSONObject lowb = new BasicBSONObject();
        lowb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaClName.getLowMonth());
        BSONObject upperb = new BasicBSONObject();
        upperb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaClName.getUpperMonth());

        options = new BasicBSONObject();
        options.put("LowBound", lowb);
        options.put("UpBound", upperb);
        System.out.println("INFO:attaching collection:" + cl.getFullName());
        logger.info("attaching cl:cl=" + cl.getFullName() + ",options=" + options.toString());
        SdbHelper.attachCL(metaHistoryCL, cl.getFullName(), options);
    }

    private void createSubCL(Date createDate) throws ScmToolsException {
        MetaCLNameInfo metaClName = wsInfo.getMetaCLName(createDate);
        BSONObject options = getFileCLOption(metaClName.getClName());
        System.out.println("INFO:creating collection:" + metaCS.getName() + "."
                + metaClName.getClName());
        logger.info("creating cl:cl=" + metaCS.getName() + "." + metaClName.getClName()
        + ",options=" + options.toString());
        DBCollection cl = SdbHelper.createCL(metaCS, metaClName.getClName(), options);

        BSONObject indexDef = new BasicBSONObject();
        indexDef.put(FieldName.FIELD_CLFILE_ID, 1);
        SdbHelper.createIdx(cl, "idx_" + cl.getName() + "_" + FieldName.FIELD_CLFILE_ID,
                indexDef.toString(), true, false);

        indexDef = new BasicBSONObject();
        indexDef.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, 1);
        SdbHelper.createIdx(cl, "idx_" + cl.getName() + "_" + FieldName.FIELD_CLFILE_FILE_DATA_ID,
                indexDef.toString(), false, false);

        BSONObject lowb = new BasicBSONObject();
        lowb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaClName.getLowMonth());
        BSONObject upperb = new BasicBSONObject();
        upperb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, metaClName.getUpperMonth());

        options = new BasicBSONObject();
        options.put("LowBound", lowb);
        options.put("UpBound", upperb);
        System.out.println("INFO:attaching collection:" + cl.getFullName());
        logger.info("attaching cl:cl=" + cl.getFullName() + ",options=" + options.toString());
        SdbHelper.attachCL(metaCL, cl.getFullName(), options);
    }

    private BSONObject getFileCLOption(String clName) {
        BSONObject key = new BasicBSONObject(FieldName.FIELD_CLFILE_ID, 1);
        BSONObject options = new BasicBSONObject();
        options.put("ShardingType", "hash");
        options.put("ShardingKey", key);
        options.put("Compressed", true);
        options.put("CompressionType", "lzw");
        options.put("ReplSize", -1);
        options.put("AutoSplit", true);
        return options;
    }

    public void close() {
        SdbHelper.closeCursorAndDb(db);
    }

    public int getGeneratedCount() {
        return count;
    }

    public static void main(String[] args) {
        Date d = new Date(1501832954000L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(sdf.format(d));
    }

}
