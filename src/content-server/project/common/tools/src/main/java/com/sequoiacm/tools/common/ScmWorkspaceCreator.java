package com.sequoiacm.tools.common;

import java.util.Date;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.element.ScmWorkspaceInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class ScmWorkspaceCreator {
    private Sequoiadb db;
    private static Logger logger = LoggerFactory.getLogger(ScmWorkspaceCreator.class);

    public ScmWorkspaceCreator(Sequoiadb db) {
        this.db = db;
    }

    public ScmWorkspaceCreator(String mainSiteDbUrl, String user, String passwd)
            throws ScmToolsException {
        db = SdbHelper.connectUrls(mainSiteDbUrl, user, passwd);
    }

    public void createWorkspace(ScmWorkspaceInfo wsInfo) throws ScmToolsException {
        // check domain and sitename valid
        System.out.println("Start to create worksapce:"+wsInfo.getName());
        logger.info("Start to create worksapce:"+wsInfo.getName());
        ScmMetaMgr mg = new ScmMetaMgr(db);
        BasicBSONList dataLocationList = wsInfo.getDataLocationBSON();
        System.out.println("Checking workspace locations");
        for (Object location : dataLocationList) {
            int siteId = (int) SdbHelper.getValueWithCheck((BSONObject) location,
                    FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
            ScmSiteInfo siteInfo = mg.getSiteInfoById(siteId);
            if (siteInfo == null) {
                logger.error("Site is not exist,id:" + siteId);
                throw new ScmToolsException("Site is not exist,id:" + siteId,
                        ScmExitCode.SCM_NOT_EXIST_ERROR);
            }
            siteInfo.validateDataLocation((BSONObject) location);
        }
        ScmSiteInfo metaSiteInfo = mg.getSiteInfoById(wsInfo.getMetaLocationSiteId());
        if (metaSiteInfo == null) {
            logger.error("Site is not exist,id:" + wsInfo.getMetaLocationSiteId());
            throw new ScmToolsException(
                    "Site is not exist,id:" + wsInfo.getMetaLocationSiteId(),
                    ScmExitCode.SCM_NOT_EXIST_ERROR);
        }
        SdbHelper.checkSdbDomainExist(metaSiteInfo.getMetaUrlStr(), metaSiteInfo.getMetaUser(),
                metaSiteInfo.getMetaDecryptPasswd(), wsInfo.getMetaLocationSiteDomain());

        // insert to workspace cl
        System.out.println("Inserting worksapce to collection:" + SdbHelper.CL_WORKSPACE);
        CollectionSpace sysCS = SdbHelper.getCS(db, SdbHelper.CS_SYS);
        if (sysCS == null) {
            logger.error("root site " + SdbHelper.CS_SYS + " cs not exist");
            throw new ScmToolsException("root site " + SdbHelper.CS_SYS + " cs not exist",
                    ScmExitCode.SCM_NOT_EXIST_ERROR);
        }
        DBCollection wsCL = SdbHelper.getCL(sysCS, SdbHelper.CL_WORKSPACE);
        if (wsCL == null) {
            logger.error("root site " + SdbHelper.CL_WORKSPACE + " cl not exist");
            throw new ScmToolsException("root site " + SdbHelper.CL_WORKSPACE + " cl not exist",
                    ScmExitCode.SCM_NOT_EXIST_ERROR);
        }

        wsInfo.setId(SdbHelper.generateCLId(db, SdbHelper.CS_SYS, SdbHelper.CL_WORKSPACE));
        SdbHelper.insert(wsCL, wsInfo.toBSON());
        // create workspace meta cs & cl

        Sequoiadb metaLocationDb = null;
        try {
            metaLocationDb = SdbHelper.connectUrls(metaSiteInfo.getMetaUrlStr(),
                    metaSiteInfo.getMetaUser(), metaSiteInfo.getMetaDecryptPasswd());
            BSONObject csOption = (BSONObject) JSON
                    .parse("{Domain:'" + wsInfo.getMetaLocationSiteDomain() + "'}");
            System.out.println(
                    "Creating workspace meta collectionspace:" + wsInfo.getName() + SdbHelper.CS_META_WS_TAIL);
            logger.info("Creating workspace meta collectionspace:" + wsInfo.getName() + SdbHelper.CS_META_WS_TAIL
                    + ",options:" + csOption.toString());
            CollectionSpace metaWsCS = SdbHelper.createCS(metaLocationDb,
                    wsInfo.getName() + SdbHelper.CS_META_WS_TAIL, csOption);

            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_DIR);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_DIR);
            DBCollection dirCL = SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_DIR);
            String dirIdxKey = String.format("{%s:1,%s:1}", FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID,
                    FieldName.FIELD_CLDIR_NAME);
            SdbHelper.createIdx(dirCL, "idx_name_pid", dirIdxKey, true, true);
            dirIdxKey = String.format("{%s:1}", FieldName.FIELD_CLDIR_ID);
            SdbHelper.createIdx(dirCL, "idx_id", dirIdxKey, true, true);
            BasicBSONObject rootDir = new BasicBSONObject();
            Date createTime = new Date();
            rootDir.put(FieldName.FIELD_CLDIR_CREATE_TIME, createTime.getTime());
            rootDir.put(FieldName.FIELD_CLDIR_ID, CommonDefine.Directory.SCM_ROOT_DIR_ID);
            rootDir.put(FieldName.FIELD_CLDIR_NAME, CommonDefine.Directory.SCM_ROOT_DIR_NAME);
            rootDir.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID,  CommonDefine.Directory.SCM_ROOT_DIR_PARENT_ID);
            rootDir.put(FieldName.FIELD_CLDIR_UPDATE_TIME, createTime.getTime());
            rootDir.put(FieldName.FIELD_CLDIR_UPDATE_USER, "admin");
            rootDir.put(FieldName.FIELD_CLDIR_USER, "admin");
            SdbHelper.insert(dirCL, rootDir);

            String relOptionStr = String.format("{ShardingType:'hash',ShardingKey:{%s:1},AutoSplit:true}",
                    FieldName.FIELD_CLREL_DIRECTORY_ID);
            BSONObject relOption = (BSONObject) JSON.parse(relOptionStr);
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_REL);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_REL+ ",options:"
                    + relOptionStr);
            DBCollection relCL = SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_REL, relOption);
            String relIdxKey = String.format("{%s:1,%s:1}", FieldName.FIELD_CLREL_DIRECTORY_ID,
                    FieldName.FIELD_CLREL_FILENAME);
            SdbHelper.createIdx(relCL, "idx_name_pid", relIdxKey, true, true);

            // CLASS
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_CLASS);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_CLASS);
            DBCollection classCl = SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_CLASS, null);
            BSONObject classIdx = new BasicBSONObject();
            classIdx.put(FieldName.Class.FIELD_NAME, 1);
            logger.info("creating index:cslName={}.{},key={},isUnique={},enforced={}",
                    classCl.getFullName(), classIdx.toString(), true, true);
            SdbHelper.createIdx(classCl, "idx_class_name", classIdx.toString(), true, true);

            // ATTRIBUTE
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_ATTRIBUTE);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_ATTRIBUTE);
            DBCollection attrCl = SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_ATTRIBUTE, null);
            BSONObject attrIdx = new BasicBSONObject();
            attrIdx.put(FieldName.Attribute.FIELD_NAME, 1);
            logger.info("creating index:cslName={}.{},key={},isUnique={},enforced={}",
                    attrCl.getFullName(), attrIdx.toString(), true, true);
            SdbHelper.createIdx(attrCl, "idx_attr_name", attrIdx.toString(), true, true);

            // CLASS_ATTR_REL
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_CLASS_ATTR_REL);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_CLASS_ATTR_REL);
            DBCollection attrAttrRelCl = SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_CLASS_ATTR_REL, null);
            BSONObject classAttrRelIdx = new BasicBSONObject();
            classAttrRelIdx.put(FieldName.ClassAttrRel.FIELD_CLASS_ID, 1);
            classAttrRelIdx.put(FieldName.ClassAttrRel.FIELD_ATTR_ID, 1);
            logger.info("creating index:cslName={}.{},key={},isUnique={},enforced={}",
                    attrAttrRelCl.getFullName(), classAttrRelIdx.toString(), true, true);
            SdbHelper.createIdx(attrAttrRelCl, "idx_rel_id", classAttrRelIdx.toString(), true, true);

            BSONObject fileOption = (BSONObject) JSON
                    .parse("{ShardingType:'range', ShardingKey:{create_month:1}, IsMainCL:true}");
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_FILE);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_FILE+ ",options:"
                    + fileOption.toString());
            SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_FILE, fileOption);

            // FILE_HISTORY
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_FILE_HISTORY);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_FILE_HISTORY + ",options:"
                    + fileOption);
            DBCollection fileHistory = SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_FILE_HISTORY,
                    fileOption);
            SdbHelper.createIdx(fileHistory, "idx_file_history_id",
                    "{id:1, major_version:1, minor_version:1}", false, false);

            // BREAKPOINT_FILE
            BSONObject breakpointFileOption = (BSONObject) JSON
                    .parse("{ShardingType:'hash',ShardingKey:{file_name:1},AutoSplit:true, EnsureShardingIndex:false}");
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_BREAKPOINT_FILE);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_BREAKPOINT_FILE + ",options:"
                    + breakpointFileOption);
            DBCollection breakpointFile =  SdbHelper.createCL(
                    metaWsCS, SdbHelper.CL_WS_BREAKPOINT_FILE, breakpointFileOption);
            SdbHelper.createIdx(breakpointFile, "file_name_index",
                    "{file_name:1}", true, false);

            // TRANSACTION_LOG
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_TRANSACTION_LOG);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_TRANSACTION_LOG);
            SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_TRANSACTION_LOG);

            // BATCH
            System.out.println("Creating workspace meta collection:" + SdbHelper.CL_WS_BATCH);
            logger.info("Creating workspace meta collection:" + SdbHelper.CL_WS_BATCH);
            BSONObject key = new BasicBSONObject(FieldName.Batch.FIELD_ID, 1);
            BSONObject batchOptions = new BasicBSONObject();
            batchOptions.put("ShardingType", "hash");
            batchOptions.put("ShardingKey", key);
            batchOptions.put("Compressed", true);
            batchOptions.put("CompressionType", "lzw");
            batchOptions.put("ReplSize", -1);
            batchOptions.put("AutoSplit", true);
            SdbHelper.createCL(metaWsCS, SdbHelper.CL_WS_BATCH, batchOptions);

        }
        catch (ScmToolsException e) {
            logger.error("create workspace:" + wsInfo.getName()
            + " cs & cl failed,removing workspace's record in " + SdbHelper.CL_WORKSPACE,
            e);
            SdbHelper.remove(wsCL, wsInfo.toBSON());
            throw e;
        }
        finally {
            SdbHelper.closeCursorAndDb(metaLocationDb);
        }
        System.out.println("Create workspace success:" + wsInfo.getName());
        logger.info("Create workspace success:" + wsInfo.getName());
    }
}
