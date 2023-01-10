package com.sequoiacm.config.framework.site.dao;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.site.metasource.SiteMetaService;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteBsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteUpdator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class UpdateSiteDao {
    @Autowired
    private SiteMetaService siteMetaService;

    @Autowired
    private Metasource metaSource;

    @Autowired
    private DefaultVersionDao versionDao;


    public ScmConfOperateResult update(SiteUpdator updator) throws ScmConfigException {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        Transaction transaction = metaSource.createTransaction();
        try {
            transaction.begin();
            String siteName = updator.getSiteName();
            TableDao table = siteMetaService.getSysSiteTable(transaction);
            BSONObject match = new BasicBSONObject(FieldName.FIELD_CLSITE_NAME, siteName);
            BSONObject siteBSON = table.queryOne(match, null, null);
            if (siteBSON != null) {
                SiteConfig siteConfig = (SiteConfig) new SiteBsonConverter()
                        .convertToConfig(siteBSON);
                BSONObject newSiteConfig = table.updateAndReturnNew(
                        new BasicBSONObject(FieldName.FIELD_CLSITE_ID, siteConfig.getId()),
                        new BasicBSONObject(SequoiadbHelper.DOLLAR_SET, new BasicBSONObject(
                                FieldName.FIELD_CLSITE_STAGE_TAG, updator.getUpdateStageTag())));

                Integer newVersion = versionDao.increaseVersion(ScmConfigNameDefine.SITE,
                        siteConfig.getName(), transaction);
                ScmConfEvent event = createEvent(
                        (SiteConfig) new SiteBsonConverter().convertToConfig(newSiteConfig),
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
        SiteNotifyOption notifyOption = new SiteNotifyOption(siteConfig.getName(), newVersion,
                EventType.UPDATE);
        return new ScmConfEventBase(ScmConfigNameDefine.SITE, notifyOption);
    }
}
