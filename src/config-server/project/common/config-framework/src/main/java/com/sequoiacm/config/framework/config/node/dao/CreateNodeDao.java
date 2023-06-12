package com.sequoiacm.config.framework.config.node.dao;

import com.sequoiacm.config.framework.config.node.metasource.NodeMetaService;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeConfig;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeFilter;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeNotifyOption;
import com.sequoiacm.infrastructure.lock.ScmLock;

@Component
public class CreateNodeDao {
    private Logger logger = LoggerFactory.getLogger(CreateNodeDao.class);
    @Autowired
    private Metasource metasource;

    @Autowired
    private NodeMetaService nodeMetaService;

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;
    @Autowired
    private DefaultVersionDao versionDao;

    public ScmConfOperateResult create(NodeConfig config) throws ScmConfigException {
        logger.info("start create content server node: nodeName={}", config.getName());
        NodeConfig nodeConfig = createContentNode(config);
        logger.info("create content server node success: nodeName={}", config.getName());
        ScmConfEvent event = createNodeEvent(config.getName());
        return new ScmConfOperateResult(nodeConfig, event);
    }

    private NodeConfig createContentNode(NodeConfig config) throws ScmConfigException {
        Transaction transaction = null;
        ScmLock lock = ScmLockManager.getInstance()
                .acquiresLock(ScmLockPathFactory.createNodeConfOpLockPath());
        try {
            transaction = metasource.createTransaction();
            TableDao contentServerTableDao = nodeMetaService.getContentServerTableDao(transaction);
            // host format: ip or hostname
            String host = config.getHostName();
            NodeFilter nodeFilter = new NodeFilter(host, config.getPort());
            if (contentServerTableDao.count(nodeFilter.asSdbCondition()) != 0) {
                throw new ScmConfigException(ScmConfError.NODE_EXIST,
                        "node is already exist: filter=" + nodeFilter.asSdbCondition());
            }

            int nodeId = contentServerTableDao.generateId();
            config.setId(nodeId);
            transaction.begin();
            try {
                contentServerTableDao.insert(configEntityTranslator.toConfigBSON(config));
            }
            catch (MetasourceException e) {
                if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                    throw new ScmConfigException(ScmConfError.NODE_EXIST,
                            "node is already exist:nodeName=" + config.getName(), e);
                }
                throw e;
            }
            lock.unlock();
            lock = null;

            versionDao.createVersion(ScmBusinessTypeDefine.NODE, config.getName(), transaction);
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
                    "failed to createnode:nodeName=" + config.getName(), e);
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

    private ScmConfEvent createNodeEvent(String nodeName) {
        NodeNotifyOption notifycation = new NodeNotifyOption(nodeName, 1);
        return new ScmConfEvent(ScmBusinessTypeDefine.NODE, EventType.CREATE, notifycation);
    }
}
