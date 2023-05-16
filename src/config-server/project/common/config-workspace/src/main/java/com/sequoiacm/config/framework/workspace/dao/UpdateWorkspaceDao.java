package com.sequoiacm.config.framework.workspace.dao;

import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.framework.workspace.checker.DataLocationConfigChecker;
import com.sequoiacm.config.framework.workspace.metasource.SysWorkspaceHistoryTableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceInstance;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.workspace.metasource.SysWorkspaceTableDao;
import com.sequoiacm.config.framework.workspace.metasource.WorkspaceMetaSerivce;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverterMgr;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;

import java.util.List;
import java.util.Map;

@Component
@EnableScmServiceDiscoveryClient
public class UpdateWorkspaceDao {

    @Autowired
    private WorkspaceMetaSerivce workspaceMetaservice;

    @Autowired
    private Metasource metasource;

    @Autowired
    private BsonConverterMgr bsonConverterMgr;

    @Autowired
    private ScmServiceDiscoveryClient scmServiceDiscoveryClient;

    @Autowired
    private DataLocationConfigChecker dataLocationConfigChecker;

    public ScmConfOperateResult update(WorkspaceUpdator updator) throws ScmConfigException {
        ScmConfOperateResult opRes = new ScmConfOperateResult();
        checkNodeVersions();

        ScmLock lock = null;
        Transaction transaction = metasource.createTransaction();
        try {
            transaction.begin();
            SysWorkspaceTableDao table = workspaceMetaservice.getSysWorkspaceTable(transaction);
            SysWorkspaceHistoryTableDao workspaceHistoryTable = workspaceMetaservice
                    .getSysWorkspaceHistoryTable(transaction);

            BSONObject matcher;
            if (updator.getMatcher() != null) {
                // content-server 发送的 matcher 是一个完整的记录
                // fulltext 发送的 matcher 是 wsName + {external_data.fulltext_sch_name}
                matcher = updator.getMatcher();
            }
            else {
                matcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME,
                        updator.getWsName());
            }

            lock = ScmLockManager.getInstance().acquiresLock(
                    ScmLockPathFactory.createWorkspaceConfOpLockPath(updator.getWsName()));

            BSONObject bakWsRecord = table.queryOne(matcher, null, null);
            if (bakWsRecord == null) {
                throw new ScmConfigException(ScmConfError.CLIENT_WROKSPACE_CACHE_EXPIRE,
                        "workspace cache is not latest. matcher:" + matcher);
            }

            BSONObject versionSet;
            if (bakWsRecord.get(FieldName.FIELD_CLWORKSPACE_VERSION) != null) {
                versionSet = new BasicBSONObject(SequoiadbHelper.DOLLAR_INC,
                        new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_VERSION, 1));
            }
            else {
                versionSet = new BasicBSONObject(SequoiadbHelper.DOLLAR_SET,
                        new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_VERSION, 2));
            }

            BSONObject newWsRecord = null;
            if (updator.getNewDesc() != null) {
                newWsRecord = table.updateDescription(matcher, updator.getNewDesc(), versionSet);
            }
            else if (updator.getNewSiteCacheStrategy() != null) {
                newWsRecord = table.updateSiteCacheStrategy(matcher,
                        updator.getNewSiteCacheStrategy(), versionSet);
            }
            else if (updator.getPreferred() != null) {
                newWsRecord = table.updatePreferred(matcher, updator.getPreferred(), versionSet);
            }
            else if (updator.getRemoveDataLocationId() != null) {
                newWsRecord = table.removeDataLocation(matcher, updator.getRemoveDataLocationId(),
                        versionSet);
            }
            else if (updator.getAddDataLocation() != null) {
                newWsRecord = table.addDataLocation(matcher, updator.getAddDataLocation(),
                        versionSet);
            }
            else if (updator.getUpdateDataLocation() != null) {
                newWsRecord = table.updateDataLocation(matcher, updator.getUpdateDataLocation(),
                        versionSet);
            }
            else if (updator.isEnableDirectory() != null) {
                Boolean isEnableDirectory = updator.isEnableDirectory();
                if (BsonUtils.getBoolean(bakWsRecord,
                        FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY)) {
                    if (isEnableDirectory) {
                        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                                "Workspace directory feature already is enabled, workspace ="
                                        + updator.getWsName());

                    }
                    newWsRecord = table.updateDirectory(matcher, false, versionSet);
                }
                else {
                    if (isEnableDirectory) {
                        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                                "Workspace directory feature cannot be enabled, workspace ="
                                        + updator.getWsName());
                    }
                    throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                            "Workspace directory feature already is disabled, workspace ="
                                    + updator.getWsName());
                }

            }
            else if (updator.getUpdateDomain() != null) {
                newWsRecord = table.updateMetaDomain(matcher, updator.getUpdateDomain(),
                        versionSet);
            }
            else if (updator.getAddExtraMetaCs() != null) {
                BasicBSONList extraMetaCs = (BasicBSONList) matcher
                        .get(FieldName.FIELD_CLWORKSPACE_EXTRA_META_CS);
                if (null != extraMetaCs && extraMetaCs.containsField(updator.getAddExtraMetaCs())) {
                    throw new ScmConfigException(ScmConfError.METASOURCE_RECORD_EXIST,
                            "new extra meta cs record exist");
                }
                newWsRecord = table.addExtraMetaCs(matcher, updator.getAddExtraMetaCs(),
                        versionSet);
            }
            else {
                // 可以多个工作区属性一起更新的字段
                BSONObject newWsAttribute = new BasicBSONObject();
                if (updator.getExternalData() != null) {
                    for (String key : updator.getExternalData().keySet()) {
                        newWsAttribute.put(FieldName.FIELD_CLWORKSPACE_EXT_DATA + "." + key,
                                updator.getExternalData().get(key));
                    }
                }
                if (updator.getTagRetrievalStatus() != null) {
                    newWsAttribute.put(FieldName.FIELD_CLWORKSPACE_TAG_RETRIEVAL_STATUS,
                            updator.getTagRetrievalStatus());
                }
                if (updator.getTagUpgrading() != null) {
                    newWsAttribute.put(FieldName.FIELD_CLWORKSPACE_TAG_UPGRADING,
                            updator.getTagUpgrading());
                }
                if (updator.getTagLibTable() != null) {
                    newWsAttribute.put(FieldName.FIELD_CLWORKSPACE_TAG_LIB_TABLE,
                            updator.getTagLibTable());
                }

                if (!newWsAttribute.isEmpty()) {
                    newWsRecord = table.updateByNewAttribute(matcher, newWsAttribute, versionSet);
                }
            }

            if (newWsRecord == null) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG,
                        "workspace update noting: " + updator.getWsName());
            }

            backupWSVersion(workspaceHistoryTable, bakWsRecord);

            WorkspaceConfig wsConfig = (WorkspaceConfig) bsonConverterMgr
                    .getMsgConverter(ScmConfigNameDefine.WORKSPACE).convertToConfig(newWsRecord);

            ScmConfEvent event = createEvent(wsConfig,
                    (int) newWsRecord.get(FieldName.FIELD_CLWORKSPACE_VERSION));
            opRes.setConfig(wsConfig);
            opRes.addEvent(event);

            transaction.commit();

            return opRes;
        }
        catch (Exception e) {
            transaction.rollback();
            throw e;
        }
        finally {
            transaction.close();
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private ScmConfEvent createEvent(WorkspaceConfig wsConfig, Integer newVersion) {
        WorkspaceNotifyOption notifycation = new WorkspaceNotifyOption(wsConfig.getWsName(),
                newVersion, EventType.UPDATE);
        return new ScmConfEventBase(ScmConfigNameDefine.WORKSPACE, notifycation);
    }

    private void checkNodeVersions() throws ScmConfigException {
        List<ScmServiceInstance> instances = scmServiceDiscoveryClient.getInstances();
        // 3.2.2 版本开始支持工作区多版本，需全部工作区相关节点升级到3.2.2及以上版本才能修改工作区配置
        // 否则旧版本的业务节点无法识别和记录工作区版本，文件元数据中丢失工作区版本可能会导致无法定位到文件的具体写入位置
        String startVersion = "3.2.2";
        String version_str = "version";
        for (ScmServiceInstance node : instances) {
            Map<String, String> metaData = node.getMetadata();
            String version = metaData.get(version_str);
            if (version == null || version.isEmpty()
                    || dataLocationConfigChecker.compareVersion(version, startVersion) < 0) {
                // 排除掉下列基础服务，剩余的就是config-server，s3-server和content-server
                // 如果以后有新增服务类型，版本号一定高于3.2.2
                if (node.getServiceName().equalsIgnoreCase("ADMIN-SERVER")
                        || node.getServiceName().equalsIgnoreCase("SERVICE-CENTER")
                        || node.getServiceName().equalsIgnoreCase("MQ-SERVER")
                        || node.getServiceName().equalsIgnoreCase("FULLTEXT-SERVER")
                        || node.getServiceName().equalsIgnoreCase("SCHEDULE-SERVER")
                        || node.getServiceName().equalsIgnoreCase("AUTH-SERVER")
                        || node.getServiceName().equalsIgnoreCase("GATEWAY")
                        || node.getServiceName().equalsIgnoreCase("OM-SERVER")) {
                    continue;
                }
                throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                        "workspace cannot be modified when the version of some nodes is lower than "
                                + startVersion + ", node " + node.getServiceName() + "("
                                + node.getHost() + ":" + node.getPort() + ") version " + version);
            }
        }
    }

    private void backupWSVersion(SysWorkspaceHistoryTableDao wsHistoryTable, BSONObject bakWsRecord)
            throws MetasourceException {
        if (bakWsRecord.get(FieldName.FIELD_CLWORKSPACE_VERSION) == null) {
            bakWsRecord.put(FieldName.FIELD_CLWORKSPACE_VERSION, 1);
        }
        wsHistoryTable.insert(bakWsRecord);
    }
}
