package com.sequoiacm.config.framework.config.site.dao;

import com.sequoiacm.config.framework.config.node.metasource.NodeMetaService;
import com.sequoiacm.config.framework.config.site.metasource.SiteMetaService;
import com.sequoiacm.config.framework.config.workspace.metasource.WorkspaceMetaSerivce;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteFilter;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.infrastructure.common.ScmQueryDefine;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteNotifyOption;

@Component
public class DeleteSiteDao {
    private static final Logger logger = LoggerFactory.getLogger(DeleteSiteDao.class);

    @Autowired
    private SiteMetaService siteMetaService;

    @Autowired
    private Metasource metaSource;

    @Autowired
    private DefaultVersionDao versionDao;

    @Autowired
    private NodeMetaService nodeMetaService;

    @Autowired
    private WorkspaceMetaSerivce wsMetaSerivce;

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;

    public ScmConfOperateResult delete(ConfigFilter filter) throws ScmConfigException {
        logger.info("start to delete site:filter={}", filter);
        SiteConfig siteConfig = deleteMeta((SiteFilter) filter);
        logger.info("delete site success:filter={}", filter);
        return createOperateResult(siteConfig);

    }

    private SiteConfig deleteMeta(SiteFilter filter) throws ScmConfigException {
        Transaction transaction = null;
        try {
            transaction = metaSource.createTransaction();
            TableDao siteTable = siteMetaService.getSysSiteTable(transaction);
            BasicBSONObject cond = new BasicBSONObject();
            if (filter.getSiteName() != null) {
                cond.put(FieldName.FIELD_CLSITE_NAME, filter.getSiteName());
            }
            BSONObject siteBson = siteTable
                    .queryOne(cond, null, null);
            if (siteBson != null) {
                SiteConfig siteConfig = (SiteConfig) configEntityTranslator
                        .fromConfigBSON(ScmBusinessTypeDefine.SITE, siteBson);
                checkIsDelete(siteTable, siteConfig, transaction);

                transaction.begin();
                siteTable.delete(siteBson);
                versionDao.deleteVersion(ScmBusinessTypeDefine.SITE, siteConfig.getName(),
                        transaction);

                TableDao subscriberDao = metaSource.getSubscribersTable();
                BSONObject matcher = new BasicBSONObject();
                matcher.put(FieldName.FIELD_CLSUBSCRIBER_SERVICE_NAME,
                        siteConfig.getName().toLowerCase());
                subscriberDao.delete(matcher);
                transaction.commit();
                return siteConfig;
            }
            throw new ScmConfigException(ScmConfError.SITE_NOT_EXIST,
                    "site is not exist:filter=" + filter);

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
                    "delete site failed:filter=" + filter, e);
        }
        finally {
            if (transaction != null) {
                transaction.close();
            }
        }
    }

    private ScmConfOperateResult createOperateResult(SiteConfig siteConfig) {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        opRes.setConfig(siteConfig);
        NotifyOption siteNotify = new SiteNotifyOption(siteConfig.getName(), -1);
        opRes.addEvent(new ScmConfEvent(ScmBusinessTypeDefine.SITE, EventType.DELTE, siteNotify));
        return opRes;
    }

    private void checkIsDelete(TableDao siteTable, SiteConfig siteConfig, Transaction transaction)
            throws ScmConfigException {
        TableDao nodeTable = nodeMetaService.getContentServerTableDao(transaction);
        TableDao wsTableDao = wsMetaSerivce.getSysWorkspaceTable(transaction);

        // rootsite &&other site size 0 && node size 0 && ws size 0
        if (siteConfig.isRootSite()) {
            // branch site size 0
            BSONObject branchSiteMatcher = new BasicBSONObject(
                    FieldName.FIELD_CLSITE_MAINFLAG, false);
            MetaCursor siteCursor = null;
            try {
                siteCursor = siteTable.query(branchSiteMatcher, null, null);
                if (siteCursor.hasNext()) {
                    BSONObject siteRecord = siteCursor.getNext();
                    throw new ScmConfigException(ScmConfError.SITE_EXIST,
                            "branch site exist, can not delete root site, branchSite ="
                                    + siteRecord);
                }
            }
            finally {
                if (siteCursor != null) {
                    siteCursor.close();
                }
            }

            // node size 0
            MetaCursor nodeCursor = null;
            try {
                nodeCursor = nodeTable.query(new BasicBSONObject(), null, null);
                if (nodeCursor.hasNext()) {
                    BSONObject contentNode = nodeCursor.getNext();
                    throw new ScmConfigException(ScmConfError.NODE_EXIST,
                            "content node exist, can not delete root site, contentNode ="
                                    + contentNode);
                }
            }
            finally {
                if (nodeCursor != null) {
                    nodeCursor.close();
                }
            }

            // ws size 0
            MetaCursor wsCursor = null;
            try {
                wsCursor = wsTableDao.query(new BasicBSONObject(), null, null);
                while (wsCursor.hasNext()) {
                    BSONObject workSpace = wsCursor.getNext();
                    throw new ScmConfigException(ScmConfError.WORKSPACE_EXIST,
                            "workspace exist, can not delete root site, workSpace =" + workSpace);
                }
            }
            finally {
                if (wsCursor != null) {
                    wsCursor.close();
                }
            }
        }
        // brachsite && node size 0 in the site && ws's data location the
        // site size 0
        else {
            // node size 0 in the site
            MetaCursor nodeCursor = null;
            BSONObject siteIdMatcher = new BasicBSONObject(FieldName.FIELD_CLCONTENTSERVER_SITEID,
                    siteConfig.getId());
            try {
                nodeCursor = nodeTable.query(siteIdMatcher, null, null);
                if (nodeCursor.hasNext()) {
                    BSONObject contentNode = nodeCursor.getNext();
                    throw new ScmConfigException(ScmConfError.NODE_EXIST,
                            "content node exist in the branch site, contentNode =" + contentNode);
                }
            }
            finally {
                if (nodeCursor != null) {
                    nodeCursor.close();
                }
            }

            // ws's data location the site size 0
            // {"data_location":{ "$elemMatch":{"site_id": siteId}}
            BSONObject wsDataMatcher = new BasicBSONObject(
                    FieldName.FIELD_CLWORKSPACE_DATA_LOCATION,
                    new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_ELEMMATCH,
                            new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID,
                                    siteConfig.getId())));
            MetaCursor wsCursor = null;
            try {
                wsCursor = wsTableDao.query(wsDataMatcher, null, null);
                if (wsCursor.hasNext()) {
                    BSONObject workSpace = wsCursor.getNext();
                    throw new ScmConfigException(ScmConfError.WORKSPACE_EXIST,
                            "workspace exist, data source loaction in the site: workSpace ="
                                    + workSpace);
                }
            }
            finally {
                if (wsCursor != null) {
                    wsCursor.close();
                }
            }
        }
    }

}
