package com.sequoiacm.config.framework.config.site.dao;

import java.util.List;

import com.sequoiacm.config.framework.common.IDGeneratorDao;
import com.sequoiacm.config.framework.config.site.metasource.SiteMetaService;
import com.sequoiacm.config.framework.config.site.tool.ScmSiteTool;
import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.common.CheckRuleUtils;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteFilter;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteNotifyOption;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Component
public class CreateSiteDao {
    private static final Logger logger = LoggerFactory.getLogger(CreateSiteDao.class);

    @Autowired
    private SiteMetaService siteMetaService;

    @Autowired
    private Metasource metaSource;

    @Autowired
    private DefaultVersionDao versionDao;

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;

    @Autowired
    private GetSiteDao getSiteDao;

    @Autowired
    private IDGeneratorDao idGeneratorDao;

    public ScmConfOperateResult create(SiteConfig config) throws ScmConfigException {
        logger.info("start to create site:{}", config.getName());
        String siteName = config.getName();
        boolean checkResult = CheckRuleUtils.isConformSiteNameRule(siteName);
        if(!checkResult){
            throw new ScmConfigException(ScmConfError.INVALID_ARG, "failed to resolve name:" + siteName + ",because " + siteName + " does not conform to host name specification");
        }
        SiteConfig siteRespConfig = createSite(config);
        logger.info("create site success:{}", siteRespConfig.getName());
        ScmConfEvent event = createSiteEvent(siteRespConfig.getName());
        return new ScmConfOperateResult(siteRespConfig, event);
    }

    private SiteConfig createSite(SiteConfig config) throws ScmConfigException {
        List<Config> allSiteConfigs = getSiteDao.get(new SiteFilter());
        ScmSiteTool.checkSiteConfigValid(allSiteConfigs, config);

        Transaction transaction = null;
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresLock(ScmLockPathFactory.createSiteConfOpLockPath());
        try {
            transaction = metaSource.createTransaction();
            TableDao sysSiteTabledao = siteMetaService.getSysSiteTable(transaction);

            if (config.isRootSite()) {
                BSONObject matcherRootSite = new BasicBSONObject(
                        FieldName.FIELD_CLSITE_MAINFLAG, true);
                BSONObject rootSite = sysSiteTabledao.queryOne(matcherRootSite, null, null);
                if (rootSite != null) {
                    throw new ScmConfigException(ScmConfError.SITE_EXIST,
                            "root site already exist, isRootSite =" + config.isRootSite());
                }
            }

            // generate site id
            int siteId = idGeneratorDao.getNewId(MetaSourceDefine.IdType.SITE,
                    sysSiteTabledao::generateId);
            config.setId(siteId);
            // insert site record
            transaction.begin();

            BSONObject siteRecord = configEntityTranslator.toConfigBSON(config);
            try {
                sysSiteTabledao.insert(siteRecord);
            }
            catch (MetasourceException e) {
                if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.SITE_EXIST,
                            "site exist:siteName=" + config.getName(), e);
                }
                throw e;
            }

            lock.unlock();
            lock = null;

            // insert site version record
            versionDao.createVersion(ScmBusinessTypeDefine.SITE, config.getName(), transaction);

            transaction.commit();
        }
        catch (ScmConfigException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
        catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to createsite:siteName=" + config.getName(), e);
        }
        finally {
            if (transaction != null) {
                transaction.close();
            }
            if (lock != null) {
                lock.unlock();
            }
        }
        return config;
    }

    private ScmConfEvent createSiteEvent(String siteName) throws ScmConfigException {
        SiteNotifyOption notifyOption = new SiteNotifyOption(siteName, 1);
        return new ScmConfEvent(ScmBusinessTypeDefine.SITE, EventType.CREATE, notifyOption);
    }

}
