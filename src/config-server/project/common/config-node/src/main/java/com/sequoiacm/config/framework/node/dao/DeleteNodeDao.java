package com.sequoiacm.config.framework.node.dao;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.node.metasource.NodeMetaService;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeBsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeConfig;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeFilter;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeNotifyOption;

@Component
public class DeleteNodeDao {
    private Logger logger = LoggerFactory.getLogger(DeleteNodeDao.class);
    @Autowired
    private Metasource metasource;

    @Autowired
    private NodeMetaService nodeMetaService;

    @Autowired
    private DefaultVersionDao versionDao;

    public ScmConfOperateResult delete(NodeFilter filter) throws ScmConfigException {
        logger.info("start to delete content node:filter={}", filter.toBSONObject());
        NodeConfig nodeConfig = deleteMeta(filter);
        logger.info("delete content node success:filter={}", filter.toBSONObject());
        return createOperateResult(nodeConfig);
    }

    private NodeConfig deleteMeta(NodeFilter filter) throws ScmConfigException {
        Transaction transaction = null;
        try {
            transaction = metasource.createTransaction();
            TableDao nodeTable = nodeMetaService.getContentServerTableDao(transaction);

            transaction.begin();
            BSONObject nodeBson = nodeTable.queryOne(filter.toBSONObject(), null, null);
            if (nodeBson != null) {
                NodeConfig nodeConfig = (NodeConfig) new NodeBsonConverter()
                        .convertToConfig(nodeBson);

                // checkIsDelete(nodeTable, nodeConfig, transaction);

                nodeTable.delete(nodeBson);
                versionDao.deleteVersion(ScmConfigNameDefine.NODE, nodeConfig.getName(),
                        transaction);
                transaction.commit();
                return nodeConfig;
            }
            throw new ScmConfigException(ScmConfError.NODE_EXIST,
                    "content node is not exist: filter=" + filter.toBSONObject());
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
                    "delete content node failed:filter =" + filter.toBSONObject(), e);
        }
        finally {
            if (transaction != null) {
                transaction.close();
            }
        }
    }

    // private void checkIsDelete(TableDao nodeTable, NodeConfig nodeConfig,
    // Transaction transaction)
    // throws ScmConfigException {
    // // when the most last of a node in the site
    // BSONObject siteMatcher = new
    // BasicBSONObject(FieldName.FIELD_CLCONTENT_SERVER_SITE_ID,
    // nodeConfig.getSiteId());
    // long nodeCount = nodeTable.count(siteMatcher);
    // if (nodeCount == 1) {
    //
    // // workspace size 0 in the site
    // TableDao wsTable = wsMetaService.getSysWorkspaceTable(transaction);
    // BSONObject wsDataMatcher = new BasicBSONObject(
    // FieldName.FIELD_CLWORKSPACE_DATA_LOCATION,
    // new BasicBSONObject(ScmQueryDefine.SEQUOIADB_MATCHER_ELEMMATCH,
    // new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID,
    // nodeConfig.getSiteId())));
    // MetaCursor wsCursor = null;
    // try {
    // wsCursor = wsTable.query(wsDataMatcher, null, null);
    // if (wsCursor.hasNext()) {
    // BSONObject wsRecord = wsCursor.getNext();
    // throw new ScmConfigException(ScmConfError.WORKSPACE_EXIST,
    // "workspace exist in the site, the last content node can not delete in the
    // site, workspace ="
    // + wsRecord);
    // }
    // }
    // finally {
    // if (wsCursor != null) {
    // wsCursor.close();
    // }
    // }
    //
    // }
    //
    // }

    private ScmConfOperateResult createOperateResult(NodeConfig nodeConfig) {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        opRes.setConfig(nodeConfig);
        NotifyOption notifycation = new NodeNotifyOption(nodeConfig.getName(), -1, EventType.DELTE);
        opRes.setEvent(new ScmConfEventBase(ScmConfigNameDefine.NODE, notifycation));
        return opRes;
    }

}
