package com.sequoiacm.contentserver;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.contentserver.common.ScmPasswordRewriter;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.job.ScmJobManager;
import com.sequoiacm.contentserver.job.ScmTaskManager;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmManifestParser;
import com.sequoiacm.infrastructure.common.ScmManifestParser.ManifestInfo;

public class ScmServer {
    private static final Logger logger = LoggerFactory.getLogger(ScmServer.class);
    private String hostName = null;
    private int port = 0;

    public ScmServer() throws Exception {
        // force to initial configure.
        // ScmAudit.getInstance().init(PropertiesUtils.getAuditMask());
        ScmContentServer cs = ScmContentServer.getInstance();
        hostName = cs.getHostName();
        if (!ScmSystemUtils.isLocalHost(hostName)) {
            throw new ScmSystemException("hostName is not in this machine:hostName=" + hostName);
        }
        port = cs.getPort();

        ScmIdGenerator.FileId.init(0, cs.getId());
        logger.info("hostName=" + hostName + ",port=" + port);
    }

    public void startService() throws InterruptedException, ScmServerException {
        logger.info("restore task");
        ScmTaskManager.getInstance().restore();

        // rewrite password
        ScmPasswordRewriter.getInstance().rewriteSiteTable();

        // start background job
        ScmJobManager.getInstance().startLogResourceJob();
        ScmJobManager.getInstance().startRollbackJob();

        logger.info("start SequoiaCM success:pid=" + ScmSystemUtils.getPid());

        PropertiesUtils.logSysConf();

        // set scm start time,reset scm status,make sure format is
        // consistent with 'com.sequoiacm.client.element.ScmProcessInfo'
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PropertiesUtils.setInternalProperty(PropertiesDefine.PROPERTY_SCM_START_TIME,
                sdf.format(new Date()));
        PropertiesUtils.setInternalProperty(PropertiesDefine.PROPERTY_SCM_STATUS,
                CommonDefine.ScmProcessStatus.SCM_PROCESS_STATUS_RUNING);

        // init scm strategy
        logger.info("init strategy");
        initStrategy();
    }

    public static void loadConfiguration() throws ScmServerException {
        PropertiesUtils.loadSysConfig();
    }

    public void start() throws IOException, ScmServerException {
        try {
            startService();
        }
        catch (Exception e) {
            logger.error("server exit with error", e);
            System.exit(-1);
        }
    }

    public static void loadVersionAndStatus() throws ScmServerException {
        ManifestInfo manifestInfo;
        try {
            manifestInfo = ScmManifestParser.getManifestInfoFromJar(ScmServer.class);
        }
        catch (IOException e) {
            throw new ScmSystemException("Failed to load manifest", e);
        }

        String revision = manifestInfo.getGitCommitIdOrSvnRevision();
        if (revision != null) {
            addInternalProperty(PropertiesDefine.PROPERTY_SCM_REVISION, revision);
        }

        String version = manifestInfo.getScmVersion();
        if (version != null) {
            addInternalProperty(PropertiesDefine.PROPERTY_SCM_VERSION, version);
        }

        String compTime = manifestInfo.getBuildTime();
        if (compTime != null) {
            addInternalProperty(PropertiesDefine.PROPERTY_SCM_COMPILE_TIME, compTime);
        }

        // set starting
        PropertiesUtils.setInternalProperty(PropertiesDefine.PROPERTY_SCM_STATUS,
                CommonDefine.ScmProcessStatus.SCM_PROCESS_STATUS_STARTING);

    }

    private static void addInternalProperty(String k, String v) {
        if (v != null) {
            PropertiesUtils.setInternalProperty(k, v);
        }
        else {
            logger.warn("failed to load properties from manifest,value is null:key={}", k);
        }
    }

    private static void initStrategy() throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        List<BSONObject> strategyList = contentServer.getMetaService().getAllStrategyInfo();
        ScmStrategyMgr.getInstance().init(strategyList, contentServer.getMainSite());
    }
}
