package com.sequoiacm.tools.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.tools.element.ScmLobInfo;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.element.ScmWorkspaceInfo;
import com.sequoiacm.tools.element.TimeArgWrapper;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.printor.ScmLobInfoPrinter;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmInspector {
    private ScmSiteInfo site;
    private ScmWorkspaceInfo ws;
    private Sequoiadb mainSiteSdb;
    private Sequoiadb lobSdb;
    private Sequoiadb metaSdb;
    private DBCollection metaCL;
    private DBCollection breakpointFileCL;
    private CollectionSpace metaCS;
    private int count;
    private String csShardingType;
    private String clShardingType;
    private static Logger logger = LoggerFactory.getLogger(ScmInspector.class);

    public ScmInspector(String siteName, String wsName, ScmSdbInfo mainSiteSdbInfo)
            throws ScmToolsException {
        mainSiteSdb = SdbHelper.connectUrls(mainSiteSdbInfo.getSdbUrl(),
                mainSiteSdbInfo.getSdbUser(), mainSiteSdbInfo.getSdbPasswd());
        try {
            ScmMetaMgr mg = new ScmMetaMgr(mainSiteSdb);
            ScmSiteInfo site = mg.getSiteInfoByName(siteName);
            if (site == null) {
                logger.error("site not exists:" + siteName);
                throw new ScmToolsException("site not exists:" + siteName,
                        ScmExitCode.SCM_SITE_NOT_EXIST);
            }
            if (!site.getDataType().equals("sequoiadb")) {
                logger.error("unsupported datasource:" + site.getDataType());
                throw new ScmToolsException("unsupported datasource:" + site.getDataType(),
                        ScmExitCode.INVALID_ARG);
            }

            ScmWorkspaceInfo ws = mg.getWorkspaceInfoByName(wsName);
            if (ws == null) {
                logger.error("workspace not exists:" + wsName);
                throw new ScmToolsException("workspace not exists:" + wsName,
                        ScmExitCode.SCM_WORKSPACE_NOT_EXIST);
            }

            boolean isWsContainSite = false;
            BasicBSONList list = ws.getDataLocationBSON();
            for (Object location : list) {
                int siteId = (int) SdbHelper.getValueWithCheck((BSONObject) location,
                        FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
                if (siteId == site.getId()) {
                    isWsContainSite = true;
                    break;
                }
            }
            if (!isWsContainSite) {
                logger.error("the site does not belong to data location of the workspace,wokspace:"
                        + wsName + ",site:" + siteName);
                throw new ScmToolsException(
                        "the site does not belong to data location of the workspace,wokspace:"
                                + wsName + ",site:" + siteName, ScmExitCode.INVALID_ARG);
            }

            this.csShardingType = ws.getCsShardingType(site.getId());
            this.clShardingType = ws.getClShardingType(site.getId());

            ScmSiteInfo metaSite = mg.getSiteInfoById(ws.getMetaLocationSiteId());

            this.lobSdb = SdbHelper.connectUrls(site.getDataUrlStr(), site.getDataUser(),
                    site.getDecryptDataPasswd());

            this.metaSdb = SdbHelper.connectUrls(metaSite.getMetaUrlStr(), metaSite.getMetaUser(),
                    metaSite.getMetaDecryptPasswd());
            String metaCSName = ws.getName() + SdbHelper.CS_META_WS_TAIL;
            String metaCLName = SdbHelper.CL_WS_FILE;
            String breakpointFileCLName = SdbHelper.CL_WS_BREAKPOINT_FILE;
            metaCS = SdbHelper.getCSWithCheck(metaSdb, metaCSName);
            metaCL = SdbHelper.getCLWithCheck(metaCS, metaCLName);
            breakpointFileCL = SdbHelper.getCLWithCheck(metaCS, breakpointFileCLName);

            this.site = site;
            this.ws = ws;
        }
        catch (ScmToolsException e) {
            close();
            throw e;
        }

    }

    public void inspectAll() throws ScmToolsException {
        List<String> clList = getAllCLFullName();
        ScmLobInfoPrinter.printHead();
        int ret = ScmExitCode.SUCCESS;
        for (String clFullName : clList) {
            String[] csAndClArr = clFullName.split("\\.");
            String csName = csAndClArr[0];
            String clName = csAndClArr[1];
            if (!CheckClFullName(csName, clName)) {
                continue;
            }
            // System.out.println(csName + "." + clName);
            logger.info("inspecting cl:" + csName + "." + clName);

            try {
                inspect(csName, clName);
            }
            catch (ScmToolsException e) {
                e.printErrorMsg();
                ret = e.getExitCode();
            }
        }
        if (ret != ScmExitCode.SUCCESS) {
            throw new ScmToolsException(ret);
        }
    }

    private List<String> getAllCLFullName() throws ScmToolsException {
        List<String> retList = new ArrayList<>();
        DBCursor dbc = SdbHelper.getList(lobSdb, Sequoiadb.SDB_LIST_COLLECTIONS, null, null, null);
        while (dbc.hasNext()) {
            BSONObject obj = dbc.getNext();
            String clFullName = (String) obj.get("Name");
            if (clFullName == null) {
                logger.error("list cl occur exception,missing field 'Name',obj:" + obj);
                throw new ScmToolsException("list cl occur exception,missing field 'Name',obj:"
                        + obj, ScmExitCode.SDB_GET_LIST);
            }
            retList.add(clFullName);
        }
        return retList;
    }

    public void inspect(String lobCSName, String lobCLName) throws ScmToolsException {
        CollectionSpace lobCS = SdbHelper.getCS(lobSdb, lobCSName);
        if (lobCS == null) {
            return;
        }
        DBCollection lobCL = SdbHelper.getCL(lobCS, lobCLName);
        if (lobCL == null) {
            return;
        }

        DBCursor c = SdbHelper.listLobs(lobCL);

        try {
            while (c.hasNext()) {
                BSONObject lobBSON = c.getNext();
                ScmLobInfo lobInfo = new ScmLobInfo(lobBSON);
                BSONObject metaMatcher = new BasicBSONObject();
                metaMatcher.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, lobInfo.getOid().toString());
                metaMatcher.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST + ".$1."
                        + FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, site.getId());
                // System.out.println(matcher);
                BSONObject metaRec = SdbHelper.queryOne(metaCL, metaMatcher, null, null);

                BSONObject breakpointFileMatcher = new BasicBSONObject();
                breakpointFileMatcher.put(FieldName.BreakpointFile.FIELD_DATA_ID, lobInfo.getOid().toString());
                BSONObject breakpointFileRec = SdbHelper.queryOne(breakpointFileCL, breakpointFileMatcher, null, null);

                if (metaRec == null && breakpointFileRec == null) {
                    logger.warn("residual lob:" + lobBSON);
                    ScmLobInfoPrinter.printScmLobInfo(lobInfo, lobCL.getFullName());
                    count++;
                }
            }
        }
        catch (BaseException e) {
            throw new ScmToolsException("Failed to inspect:"
                    + SdbHelper.processSdbErrorMsg(e), ScmExitCode.SDB_QUERY_ERROR);
        }
    }

    public void close() {
        SdbHelper.closeCursorAndDb(mainSiteSdb);
        SdbHelper.closeCursorAndDb(metaSdb);
        SdbHelper.closeCursorAndDb(lobSdb);
    }

    public int getCount() {
        return count;
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
            return null;
        }

    }

    private TimeArgWrapper getTimeFromCsName(String lobCsName) throws ScmToolsException {
        if (!lobCsName.startsWith(ws.getName() + SdbHelper.CS_LOB_WS_TAIL)) {
            return null;
        }
        String csNametail = lobCsName.replace(ws.getName() + SdbHelper.CS_LOB_WS_TAIL, "");
        String regex;

        if (csShardingType.equals(ScmFiledDefine.WORKSPACE_SHARDING_NONE_STR)) {
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
            return null;
        }

    }

    private boolean CheckClFullName(String lobCsName, String lobClName) throws ScmToolsException {
        TimeArgWrapper timeWrapper;

        if (csShardingType.equals(ws.getSmaller(csShardingType, clShardingType))) {

            timeWrapper = getTimeFromCsName(lobCsName);
        }
        else {

            timeWrapper = getTimeFromCLName(lobClName);
        }
        if (timeWrapper == null) {
            return false;
        }
        String correctCsName = ws.getDataCsName(timeWrapper.getLower(), site.getId());
        String correctClName = ws.getDataClName(timeWrapper.getLower(), site.getId());
        if (!lobCsName.equals(correctCsName)) {

            return false;
        }
        if (!lobClName.equals(correctClName)) {

            return false;
        }
        return true;
    }

}
