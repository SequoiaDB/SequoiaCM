package com.sequoiacm.config.framework.config.site.dao;

import com.sequoiacm.config.framework.config.site.metasource.SiteMetaService;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteUpdater;


@Component
public class UpdateSiteDao {
    @Autowired
    private SiteMetaService siteMetaService;

    @Autowired
    private Metasource metaSource;

    @Autowired
    private DefaultVersionDao versionDao;

    @Autowired
    private ConfigEntityTranslator translator;


    public ScmConfOperateResult update(SiteUpdater updator) throws ScmConfigException {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        Transaction transaction = metaSource.createTransaction();
        try {
            transaction.begin();
            String siteName = updator.getSiteName();
            TableDao table = siteMetaService.getSysSiteTable(transaction);
            BSONObject match = new BasicBSONObject(FieldName.FIELD_CLSITE_NAME, siteName);
            BSONObject siteBSON = table.queryOne(match, null, null);
            if (siteBSON != null) {
                SiteConfig siteConfig = (SiteConfig) translator
                        .fromConfigBSON(ScmBusinessTypeDefine.SITE, siteBSON);
                BSONObject newSiteConfig = table.updateAndReturnNew(
                        new BasicBSONObject(FieldName.FIELD_CLSITE_ID, siteConfig.getId()),
                        new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, new BasicBSONObject(
                                FieldName.FIELD_CLSITE_STAGE_TAG, updator.getUpdateStageTag())));

                Integer newVersion = versionDao.increaseVersion(ScmBusinessTypeDefine.SITE,
                        siteConfig.getName(), transaction);
                ScmConfEvent event = createEvent(
                        (SiteConfig) translator.fromConfigBSON(ScmBusinessTypeDefine.SITE,
                                newSiteConfig),
                        newVersion);
                opRes.setConfig(siteConfig);
                opRes.addEvent(event);

                transaction.commit();

                return opRes;
            }
            throw new ScmConfigException(ScmConfError.SITE_NOT_EXIST,
                    "site is not exist:siteUpdator:" + updator);
        }
        catch (Exception e) {
            transaction.rollback();
            throw e;
        }
        finally {
            transaction.close();
        }
    }

    private ScmConfEvent createEvent(SiteConfig siteConfig, Integer newVersion) {
        SiteNotifyOption notifyOption = new SiteNotifyOption(siteConfig.getName(), newVersion);
        return new ScmConfEvent(ScmBusinessTypeDefine.SITE, EventType.UPDATE, notifyOption);
    }
}
