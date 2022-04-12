package com.sequoiacm.contentserver;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmPasswordRewriter;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.contentmodule.ContentModuleInitializer;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.job.ScmJobManager;
import com.sequoiacm.contentserver.job.ScmTaskManager;
import com.sequoiacm.contentserver.metasourcemgr.MapServerHandlerAdapter;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmContentServerInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmManifestParser;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ScmServer {
    private static final Logger logger = LoggerFactory.getLogger(ScmServer.class);
    private static ScmServer instance;
    private volatile ScmContentServerInfo contentServerInfo;

    public static ScmServer getInstance() throws ScmServerException {
        if (instance == null) {
            synchronized (ScmServer.class) {
                if (instance == null) {
                    instance = new ScmServer();
                }
            }
        }
        return instance;
    }

    private ScmServer() throws ScmServerException {
    }

    public ScmContentServerInfo getContentServerInfo() throws ScmServerException {
        if (contentServerInfo == null) {
            initLocalServerInfo();
        }
        return contentServerInfo;
    }

    private void initLocalServerInfo() throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        List<ScmContentServerInfo> localSiteNode = contentModule.getContentServerList(contentModule.getLocalSite());
        for (ScmContentServerInfo node : localSiteNode) {
            if (ScmSystemUtils.isLocalHost(node.getHostName())
                    && PropertiesUtils.getServerPort() == node.getPort()) {
                this.contentServerInfo = node;
                break;
            }
        }
        if (contentServerInfo == null) {
            throw new ScmServerException(ScmError.METASOURCE_ERROR,
                    "this server is not exist in SCM:host=" + ScmSystemUtils.getHostName()
                            + ",port=" + PropertiesUtils.getServerPort());
        }
        logger.info("content-server info: serverId={}, hostName={}, port={}, site={}",
                contentServerInfo.getId(), contentServerInfo.getHostName(),
                contentServerInfo.getPort(), contentServerInfo.getSite().getName());
    }

    public void init(ScmPrivClient privClient, ScmConfClient confClient, String siteName, BucketInfoManager bucketInfoManager, IDirService dirService)
            throws Exception {
        ContentModuleInitializer initializer = new ContentModuleInitializer(privClient, confClient,
                siteName, siteName, new MapServerHandlerAdapter(), bucketInfoManager, dirService);

        // 先执行 content-module 第一阶段的初始化
        initializer.initBizComponent();

        // 第一阶段初始化后可以从content-module拿到节点列表，将本节点信息识别出来
        initLocalServerInfo();

        // 第二节点初始化使用本节点 ID 初始化节点 ID 生成器
        initializer.initIdGenerator(contentServerInfo.getId());

        logger.info("restore task");
        ScmTaskManager.getInstance().restore();
        // rewrite password
        ScmPasswordRewriter.getInstance().rewriteSiteTable();

        // start background job
        ScmJobManager.getInstance().startLogResourceJob();
        ScmJobManager.getInstance().startRollbackJob();

        loadConfiguration();
        loadVersionAndStatus();

        logger.info("start SequoiaCM success:pid=" + ScmSystemUtils.getPid());

        PropertiesUtils.logSysConf();
        // set scm start time,reset scm status,make sure format is
        // consistent with 'com.sequoiacm.client.element.ScmProcessInfo'
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PropertiesUtils.setInternalProperty(PropertiesDefine.PROPERTY_SCM_START_TIME,
                sdf.format(new Date()));
        PropertiesUtils.setInternalProperty(PropertiesDefine.PROPERTY_SCM_STATUS,
                CommonDefine.ScmProcessStatus.SCM_PROCESS_STATUS_RUNING);
    }

    private void loadConfiguration() throws ScmServerException {
        PropertiesUtils.loadSysConfig();
    }

    private void loadVersionAndStatus() throws ScmServerException {
        ScmManifestParser.ManifestInfo manifestInfo;
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

}
