package com.sequoiacm.deploy.common;

import java.util.Arrays;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmDeployInfoMgr;
import com.sequoiacm.deploy.module.MetaSourceInfo;
import com.sequoiacm.deploy.module.SiteStrategyType;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class SequoiadbTableInitializer {
    private static final String SYSTEM_CS = "SCMSYSTEM";
    private static final String STRATEGY_CL = "STRATEGY";

    private static final String FIELD_SOURCE_SITE = "source_site";
    private static final String FIELD_TARGET_SITE = "target_site";
    private static final String FIELD_CONNECTIVITY = "connectivity";

    private static final Logger logger = LoggerFactory.getLogger(SequoiadbTableInitializer.class);
    private MetaSourceInfo metasourceInfo;
    private String systemJsonFile;
    private String auditJsonFile;
    private MetaSourceInfo auditsourceInfo;
    private static volatile SequoiadbTableInitializer instance;

    public static SequoiadbTableInitializer getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SequoiadbTableInitializer.class) {
            if (instance != null) {
                return instance;
            }
            instance = new SequoiadbTableInitializer();
            return instance;
        }
    }

    public SequoiadbTableInitializer() {
        this.metasourceInfo = ScmDeployInfoMgr.getInstance().getMetasourceInfo();
        this.auditsourceInfo = ScmDeployInfoMgr.getInstance().getAuditsourceInfo();
        CommonConfig commonConfig = CommonConfig.getInstance();
        this.systemJsonFile = commonConfig.getSdbSystemTableConfFilePath();
        this.auditJsonFile = commonConfig.getSdbAuditTableConfFilePath();
    }

    public void doInitialize(boolean dryrun) throws Exception {
        doInitialize(metasourceInfo, systemJsonFile, dryrun);
        if (ScmDeployInfoMgr.getInstance().getSiteStrategy().getType() == SiteStrategyType.NETWORK
                && !dryrun) {
            initNetSiteStrategy();
        }
        doInitialize(auditsourceInfo, auditJsonFile, dryrun);
    }

    private void initNetSiteStrategy() throws Exception {
        List<String> url = Arrays.asList(metasourceInfo.getUrl().split(","));
        Sequoiadb sdb = null;
        try {
            sdb = new Sequoiadb(url, metasourceInfo.getUser(), metasourceInfo.getPassword(),
                    new ConfigOptions());
            DBCollection strategyCl = sdb.getCollectionSpace(SYSTEM_CS).getCollection(STRATEGY_CL);
            BasicBSONObject network = new BasicBSONObject();
            network.put(FIELD_TARGET_SITE, -1);
            network.put(FIELD_SOURCE_SITE, -1);
            network.put(FIELD_CONNECTIVITY, SiteStrategyType.NETWORK.getType());
            strategyCl.insert(network);
        }
        catch (Exception e) {
            throw new Exception("failed to init site strategy:sdb=" + metasourceInfo.getUrl(), e);
        }
        finally {
            CommonUtils.closeResource(sdb);
        }
    }

    public void doUninitialize(boolean dryrun) throws Exception {
        uninitialize(metasourceInfo, systemJsonFile, dryrun);
        uninitialize(auditsourceInfo, auditJsonFile, dryrun);
    }

    public void uninitialize(MetaSourceInfo source, String csclJsonFile, boolean dryrun)
            throws Exception {
        List<String> url = Arrays.asList(source.getUrl().split(","));
        Sequoiadb sdb = null;
        try {
            if (!dryrun) {
                sdb = new Sequoiadb(url, source.getUser(), source.getPassword(),
                        new ConfigOptions());
            }
            String csClJson = CommonUtils.readContentFromLocalFile(csclJsonFile);
            BSONObject csClBson = (BSONObject) JSON.parse(csClJson);

            BasicBSONList css = (BasicBSONList) csClBson.get("collection_spaces");
            for (Object cs : css) {
                BSONObject csBSON = (BSONObject) cs;
                if (dryrun) {
                    logger.info("Collectionspace will be drop:name=" + csBSON.get("name") + ", sdb="
                            + source.getUrl());
                    continue;
                }
                SdbTools.dorpCS(sdb, (String) csBSON.get("name"));
            }
        }
        catch (Exception e) {
            throw new Exception("failed to uninitialize:sdb=" + metasourceInfo.getUrl(), e);
        }
        finally {
            CommonUtils.closeResource(sdb);
        }
    }

    private void doInitialize(MetaSourceInfo source, String csclJsonFile, boolean dryrun)
            throws Exception {
        List<String> url = Arrays.asList(source.getUrl().split(","));
        Sequoiadb sdb = null;
        try {
            if (!dryrun) {
                sdb = new Sequoiadb(url, source.getUser(), source.getPassword(),
                        new ConfigOptions());
            }
            String csClJson = CommonUtils.readContentFromLocalFile(csclJsonFile);
            BSONObject csClBson = (BSONObject) JSON.parse(csClJson);

            BasicBSONList css = (BasicBSONList) csClBson.get("collection_spaces");
            for (Object cs : css) {
                BSONObject csBSON = (BSONObject) cs;
                BSONObject csOption = (BSONObject) csBSON.get("options");
                csOption.put("Domain", metasourceInfo.getDomain());
                if (dryrun) {
                    logger.info("Collectionspace will be create:name=" + csBSON.get("name")
                            + ", sdb=" + source.getUrl());
                    continue;
                }
                SdbTools.createCS(sdb, (String) csBSON.get("name"), csOption);
            }

            BasicBSONList cls = (BasicBSONList) csClBson.get("collections");
            for (Object cl : cls) {
                BSONObject clBSON = (BSONObject) cl;
                if (dryrun) {
                    logger.info("Collection will be create:name=" + clBSON.get("csName") + "."
                            + (String) clBSON.get("clName") + ", sdb=" + source.getUrl());
                    continue;
                }
                DBCollection dbCL = SdbTools.createCL(sdb, (String) clBSON.get("csName"),
                        (String) clBSON.get("clName"), (BSONObject) clBSON.get("options"));

                BasicBSONList idxs = (BasicBSONList) clBSON.get("indexes");
                if (idxs != null) {
                    for (Object idx : idxs) {
                        BSONObject idxBSON = (BSONObject) idx;
                        SdbTools.createIdx(dbCL, (String) idxBSON.get("name"),
                                (BSONObject) idxBSON.get("indexDef"),
                                Boolean.valueOf((String) idxBSON.get("isUnique")),
                                Boolean.valueOf((String) idxBSON.get("enforced")));
                    }
                }

                BasicBSONList initRecords = (BasicBSONList) clBSON.get("init_records");
                if (initRecords != null) {
                    for (Object record : initRecords) {
                        BSONObject recordBSON = (BSONObject) record;
                        dbCL.insert(recordBSON);
                    }
                }
            }

        }
        catch (Exception e) {
            throw new Exception("failed to initialize:sdb=" + metasourceInfo.getUrl(), e);
        }
        finally {
            CommonUtils.closeResource(sdb);
        }
    }

}
