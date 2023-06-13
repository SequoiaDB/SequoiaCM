package com.sequoiacm.client.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.client.dispatcher.RestDispatcher;
import com.sequoiacm.client.element.ScmCheckConnResult;
import com.sequoiacm.client.element.ScmCheckConnTarget;
import com.sequoiacm.client.element.ScmCleanTaskConfig;
import com.sequoiacm.client.element.ScmConfProp;
import com.sequoiacm.client.element.ScmConfigPropertiesQuery;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.element.ScmOnceTransitionConfig;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmSpaceRecyclingTaskConfig;
import com.sequoiacm.client.element.ScmTransferTaskConfig;
import com.sequoiacm.client.element.lifecycle.ScmCleanTriggers;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.element.lifecycle.ScmTransitionTriggers;
import com.sequoiacm.client.element.lifecycle.ScmTrigger;
import com.sequoiacm.client.element.trace.ScmTrace;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.IOUtils;
import com.sequoiacm.infrastructure.common.XMLUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.StatisticsType;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmProcessInfo;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.common.ScmArgChecker;

/**
 * Provide ScmSystem operations.
 *
 * @since 2.1
 */
public class ScmSystem {

    /**
     * Provide scm cluster connectivity operations.
     *
     * @since 3.2.2
     */
    public static class Diagnose {

        /**
         * check scm node connectivity.
         *
         * @param srcNode
         *            srcNode ( ip:port ) .
         * @param target
         *            check target instance or service.
         * @return the node with cluster connectivity list
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmCheckConnResult> checkConnectivity(String srcNode,
                ScmCheckConnTarget target) throws ScmException {
            ScmRequestConfig config = ScmRequestConfig.custom().setConnectTimeout(2000)
                    .setSocketTimeout(300000).build();
            List<ScmCheckConnResult> ret = new ArrayList<ScmCheckConnResult>();
            RestDispatcher dispatcher = null;
            BasicBSONList resp = null;
            try {
                dispatcher = new RestDispatcher(srcNode, config);
                resp = dispatcher.getCheckConnResult(srcNode, target);
            }
            finally {
                if (dispatcher != null) {
                    dispatcher.close();
                }
            }
            for (Object o : resp) {
                BSONObject resultBson = (BSONObject) o;
                ScmCheckConnResult result = new ScmCheckConnResult(resultBson);
                ret.add(result);
            }
            return ret;
        }

        /**
         * check scm cluster connectivity.
         *
         * @param session
         *            session.
         * @return the cluster connectivity Map
         * @throws ScmException
         *             if error happens.
         */
        public static Map<ScmServiceInstance, List<ScmCheckConnResult>> checkConnectivity(
                ScmSession session) throws ScmException {
            Map<ScmServiceInstance, List<ScmCheckConnResult>> connResultMap = new HashMap<ScmServiceInstance, List<ScmCheckConnResult>>();
            List<ScmServiceInstance> serviceInstances = ScmSystem.ServiceCenter
                    .getServiceInstanceList(session, null);
            ScmCheckConnTarget target = ScmCheckConnTarget.builder().allInstance().build();
            for (ScmServiceInstance instance : serviceInstances) {
                List<ScmCheckConnResult> results = checkConnectivity(
                        instance.getIp() + ":" + instance.getPort(), target);
                connResultMap.put(instance, results);
            }
            return connResultMap;
        }
    }

    /**
     * Provide configuration operations.
     *
     * @since 2.1
     */
    public static class Configuration {

        private Configuration() {

        }

        /**
         * Reload all node business configration
         *
         * @param session
         *            Session for request.
         * @return list of results
         * @throws ScmException
         *             If error happens.
         * @since 2.1
         */
        public static List<BSONObject> reloadBizConf(ScmSession session) throws ScmException {
            return reloadBizConf(ServerScope.ALL_SITE, -1, session);
        }

        /**
         * Reload business configuration
         *
         * @param scope
         *            the scope as below ScmType.ServerScope.NODE :reload specify id of
         *            node ScmType.ServerScope.CENTER:reload specify id of site
         *            ScmType.ServerScope.ALL:reload all node
         * @param id
         *            specify the id of node,if the scope is ScmType.ServerScope.ALL,id
         *            is useless
         * @param session
         *            session for request.
         *
         * @return list of results
         * @throws ScmException
         *             If error happens
         * @since 2.1
         */
        public static List<BSONObject> reloadBizConf(ServerScope scope, int id, ScmSession session)
                throws ScmException {
            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }

            if (null == session) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (scope.getScope() == CommonDefine.NodeScope.SCM_NODESCOPE_ALL) {
                return session.getDispatcher()
                        .reloadBizConf(CommonDefine.NodeScope.SCM_NODESCOPE_ALL, -1);
            }
            else {
                return session.getDispatcher().reloadBizConf(scope.getScope(), id);
            }
        }

        /**
         * Get conf by specified key.
         *
         * @param session
         *            unauthorized session or authorized session.
         * @param key
         *            configuration item.
         * @return return the value of specified conf or null if not exist.
         * @throws ScmException
         *             If error happens
         * @since 2.2
         * @deprecated use
         *             {@link #getConfigProperties(ScmSession, ScmConfigPropertiesQuery)}
         *             instead.
         */
        @Deprecated
        public static String getConfProperty(ScmSession session, String key) throws ScmException {
            if (key == null) {
                throw new ScmInvalidArgumentException("key is null");
            }
            List<String> list = new ArrayList<String>();
            list.add(key);
            BSONObject bson = getConfProperties(session, list);
            return (String) bson.get(key);
        }

        /**
         * Get confs by specified keys.
         *
         * @param session
         *            unauthorized session or authorized session.
         * @param keys
         *            a list of conf keys.
         * @return bson object,contain conf key and value.
         * @throws ScmException
         *             If error happens
         * @since 2.2
         * @deprecated use
         *             {@link #getConfigProperties(ScmSession, ScmConfigPropertiesQuery)}
         *             instead.
         */
        @Deprecated
        public static BSONObject getConfProperties(ScmSession session, List<String> keys)
                throws ScmException {
            if (null == session) {
                throw new ScmInvalidArgumentException("session is null");
            }
            if (null == keys) {
                throw new ScmInvalidArgumentException("keys is null");
            }
            if (keys.size() <= 0) {
                throw new ScmInvalidArgumentException("keys size is 0");
            }

            BSONObject keysBSON = new BasicBSONObject();
            for (String key : keys) {
                if (key == null) {
                    throw new ScmInvalidArgumentException("keys contain null element");
                }
                keysBSON.put(key, null);
            }
            return session.getDispatcher().getConfProperties(keysBSON);
        }

        /**
         * Get config properties.
         * 
         * @param session
         *            session.
         * @param query
         *            query condition, see {@link ScmConfigPropertiesQuery}.
         * @return config property list.
         * @throws ScmException
         *             If error happens.
         */
        public static List<ScmConfProp> getConfigProperties(ScmSession session,
                ScmConfigPropertiesQuery query) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("query", query);

            BasicBSONList ret = session.getDispatcher().getConfProps(query.getTargetType(),
                    query.getTargets(), query.getProps());
            List<ScmConfProp> result = new ArrayList<ScmConfProp>(ret.size());
            for (Object obj : ret) {
                result.add(new ScmConfProp((BSONObject) obj));
            }
            return result;
        }

        /**
         * Sets the configuration of the specified nodes.
         *
         * @param session
         *            Session for request
         * @param config
         *            Specified nodes and new configuration.
         * @return Operation result.
         * @throws ScmException
         *             If error happens.
         */
        public static ScmUpdateConfResultSet setConfigProperties(ScmSession session,
                ScmConfigProperties config) throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("config", config);
            BSONObject ret = session.getDispatcher().updateConfProps(config.getTargetType(),
                    config.getTargets(), config.getUpdateProps(), config.getDeleteProps(),
                    config.isAcceptUnknownProps());
            return new ScmUpdateConfResultSet(ret);
        }

        /**
         * Sets the global configuration.
         *
         * @param session
         *            Session for request
         * @param confName
         *            Global configuration name
         * @param confValue
         *            Global configuration value
         * @throws ScmException
         *             If error happens.
         * @since 3.6.1
         */
        public static void setGlobalConfig(ScmSession session, String confName, String confValue)
                throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("key", confName);
            session.getDispatcher().setGlobalConfig(confName, confValue);
        }

        /**
         * Gets the global configuration.
         *
         * @param session
         *            Session for request
         * @param confName
         *            Global configuration name
         * @return Global configuration value, null if not exist.
         * @throws ScmException
         *             If error happens.
         * @since 3.6.1
         */
        public static String getGlobalConfig(ScmSession session, String confName)
                throws ScmException {
            checkArgNotNull("session", session);
            checkArgNotNull("key", confName);
            return session.getDispatcher().getGlobalConfig(confName);
        }

        /**
         * Gets all global configuration.
         *
         * @param session
         *            Session for request
         * @return Global configuration map, key is configuration name, value is
         *         configuration
         * @throws ScmException
         *             If error happens.
         * @since 3.6.1
         */
        public static Map<String, String> getGlobalConfig(ScmSession session) throws ScmException {
            checkArgNotNull("session", session);
            return session.getDispatcher().getGlobalConfig();
        }
    }

    /**
     * Provide Task operations.
     */
    public static class Task {
        private Task() {

        }

        /**
         * List Task
         *
         * @param ss
         *            Session for request.
         * @param condition
         *            The matching rule
         * @return task cursor
         * @throws ScmException
         *             If error happens
         * @since 2.1
         */
        public static ScmCursor<ScmTaskBasicInfo> listTask(ScmSession ss, BSONObject condition)
                throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            BsonReader reader = ss.getDispatcher().getTaskList(condition);
            ScmCursor<ScmTaskBasicInfo> cusor = new ScmBsonCursor<ScmTaskBasicInfo>(reader,
                    new BsonConverter<ScmTaskBasicInfo>() {
                        @Override
                        public ScmTaskBasicInfo convert(BSONObject obj) throws ScmException {
                            return new ScmTaskBasicInfo(obj);
                        }
                    });
            return cusor;
        }

        /**
         * List tasks which matches between the specified query condition.
         *
         * @param ss
         *            session for request.
         * @param condition
         *            the condition of query tasks
         * @param orderby
         *            the condition for sort, include: key is a property of
         *            {@link ScmAttributeName.Task}, value is -1(descending) or
         *            1(ascending)
         * @param skip
         *            skip the the specified amount of tasks, never skip if this
         *            parameter is 0.
         * @param limit
         *            return the specified amount of tasks, when limit is -1, return all
         *            the files.
         * @return task cursor for traverse
         * @throws ScmException
         *             If error happens.
         * @since 3.1
         */
        public static ScmCursor<ScmTaskBasicInfo> listTask(ScmSession ss, BSONObject condition,
                BSONObject orderby, long skip, long limit) throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            if (null == orderby) {
                throw new ScmInvalidArgumentException("orderby is null");
            }

            BsonReader reader = ss.getDispatcher().getTaskList(condition, orderby, null, skip,
                    limit);
            ScmCursor<ScmTaskBasicInfo> cursor = new ScmBsonCursor<ScmTaskBasicInfo>(reader,
                    new BsonConverter<ScmTaskBasicInfo>() {
                        @Override
                        public ScmTaskBasicInfo convert(BSONObject obj) throws ScmException {
                            return new ScmTaskBasicInfo(obj);
                        }
                    });
            return cursor;
        }

        /**
         * Start transfer file task, when the star strategy is in use.
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 2.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startTransferTaskV2 interface.
         */
        @Deprecated
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition)
                throws ScmException {
            return _startTransferTask(ws, condition, ScopeType.SCOPE_CURRENT, 0, null,
                    ScmDataCheckLevel.WEEK, false, false);

        }

        /**
         * Start transfer file task, when the star strategy is in use.
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @param scope
         *            scope type
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 2.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startTransferTaskV2 interface.
         */
        @Deprecated
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope) throws ScmException {
            return _startTransferTask(ws, condition, scope, 0, null, ScmDataCheckLevel.WEEK, false,
                    false);
        }

        /**
         * Start transfer file task.
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @param scope
         *            scope type
         * @param maxExecTime
         *            max exec time
         * @return task id
         * @throws ScmException
         *             If error happens
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startTransferTaskV2 interface.
         */
        @Deprecated
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, long maxExecTime) throws ScmException {
            return _startTransferTask(ws, condition, scope, maxExecTime, null,
                    ScmDataCheckLevel.WEEK, false, false);
        }

        /**
         * Start transfer file task
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @param scope
         *            scope type
         * @param targetSite
         *            target site name
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 2.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startTransferTaskV2 interface.
         */
        @Deprecated
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, String targetSite) throws ScmException {
            return _startTransferTask(ws, condition, scope, 0, targetSite, ScmDataCheckLevel.WEEK,
                    false, false);
        }

        /**
         * Start transfer file task use ScmTransferTaskConfig
         *
         * @param config
         *            transfer task config
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 3.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startTransferTaskV2 interface.
         */
        @Deprecated
        public static ScmId startTransferTask(ScmTransferTaskConfig config) throws ScmException {
            if (null == config) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startTransferTask(config.getWorkspace(), config.getCondition(),
                    config.getScope(), config.getMaxExecTime(), config.getTargetSite(),
                    config.getDataCheckLevel(), config.isQuickStart(), false);
        }

        /**
         * Start transfer file task use ScmTransferTaskConfig. Async count the number of
         * files, when quick start is not enabled and the version of the server is v3.6
         *
         * @param config
         *            transfer task config
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 3.6
         */
        public static ScmId startTransferTaskV2(ScmTransferTaskConfig config) throws ScmException {
            if (null == config) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startTransferTask(config.getWorkspace(), config.getCondition(),
                    config.getScope(), config.getMaxExecTime(), config.getTargetSite(),
                    config.getDataCheckLevel(), config.isQuickStart(), true);
        }

        /**
         * Start transfer file task.
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @param scope
         *            scope type
         * @param maxExecTime
         *            max exec time
         * @param targetSite
         *            target site name
         * @return task id
         * @throws ScmException
         *             If error happens
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startTransferTaskV2 interface.
         */
        @Deprecated
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, long maxExecTime, String targetSite) throws ScmException {
            return _startTransferTask(ws, condition, scope, maxExecTime, targetSite,
                    ScmDataCheckLevel.WEEK, false, false);

        }

        private static ScmId _startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, long maxExecTime, String targetSite,
                ScmDataCheckLevel dataCheckLevel, boolean quickStart, boolean isAsyncCountFile)
                throws ScmException {
            if (null == ws) {
                throw new ScmInvalidArgumentException("workspace is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }
            if (null == dataCheckLevel) {
                throw new ScmInvalidArgumentException("dataCheckLevel is null");
            }
            if (scope != ScopeType.SCOPE_CURRENT) {
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(condition);
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("invalid condition", e);
                }
            }

            ScmSession conn = ws.getSession();
            return conn.getDispatcher().MsgStartTransferTask(ws.getName(), condition,
                    scope.getScope(), maxExecTime, targetSite, dataCheckLevel.getName(), quickStart,
                    isAsyncCountFile);

        }

        /**
         * start file clean task.
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 2.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startCleanTaskV2 interface.
         */
        @Deprecated
        public static ScmId startCleanTask(ScmWorkspace ws, BSONObject condition)
                throws ScmException {
            return _startCleanTask(ws, condition, ScopeType.SCOPE_CURRENT, 0,
                    ScmDataCheckLevel.WEEK, false, false, false);
        }

        /**
         * start file clean task.
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @param scope
         *            scope type
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 2.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startCleanTaskV2 interface.
         */
        @Deprecated
        public static ScmId startCleanTask(ScmWorkspace ws, BSONObject condition, ScopeType scope)
                throws ScmException {
            return _startCleanTask(ws, condition, scope, 0, ScmDataCheckLevel.WEEK, false, false,
                    false);
        }

        /**
         * start file clean task.
         *
         * @param ws
         *            workspace
         * @param condition
         *            file matching rule
         * @param scope
         *            scope type
         * @param maxExecTime
         *            max exec time
         * @return task id
         * @throws ScmException
         *             If error happens
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startCleanTaskV2 interface.
         */
        @Deprecated
        public static ScmId startCleanTask(ScmWorkspace ws, BSONObject condition, ScopeType scope,
                long maxExecTime) throws ScmException {
            return _startCleanTask(ws, condition, scope, maxExecTime, ScmDataCheckLevel.WEEK, false,
                    false, false);
        }

        private static ScmId _startCleanTask(ScmWorkspace ws, BSONObject condition, ScopeType scope,
                long maxExecTime, ScmDataCheckLevel dataCheckLevel, boolean quickStart,
                boolean isRecycleSpace, boolean isAsyncCountFile) throws ScmException {
            if (null == ws) {
                throw new ScmInvalidArgumentException("workspace is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }
            if (null == dataCheckLevel) {
                throw new ScmInvalidArgumentException("dataCheckLevel is null");
            }
            ScmSession conn = ws.getSession();
            return conn.getDispatcher().MsgStartCleanTask(ws.getName(), condition, scope.getScope(),
                    maxExecTime, dataCheckLevel.getName(), quickStart, isRecycleSpace,
                    isAsyncCountFile);
        }

        /**
         * start file clean task use ScmCleanTaskConfig.
         *
         * @param config
         *            clean task config
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 3.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startCleanTaskV2 interface.
         */
        @Deprecated
        public static ScmId startCleanTask(ScmCleanTaskConfig config) throws ScmException {
            if (config == null) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startCleanTask(config.getWorkspace(), config.getCondition(), config.getScope(),
                    config.getMaxExecTime(), config.getDataCheckLevel(), config.isQuickStart(),
                    config.isRecycleSpace(), false);
        }

        /**
         * start file clean task use ScmCleanTaskConfig. Async count the number of
         * files, when quick start is not enabled and the version of the server is v3.6
         *
         * @param config
         *            clean task config
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 3.6
         */
        public static ScmId startCleanTaskV2(ScmCleanTaskConfig config) throws ScmException {
            if (config == null) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startCleanTask(config.getWorkspace(), config.getCondition(), config.getScope(),
                    config.getMaxExecTime(), config.getDataCheckLevel(), config.isQuickStart(),
                    config.isRecycleSpace(), true);
        }

        /**
         * start file move task use ScmMoveTaskConfig.
         *
         * @param config
         *            clean task config
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 3.1
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startMoveTaskV2 interface.
         */
        @Deprecated
        public static ScmId startMoveTask(ScmMoveTaskConfig config) throws ScmException {
            if (config == null) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startMoveTask(config.getWorkspace(), config.getCondition(), config.getScope(),
                    config.getMaxExecTime(), config.getTargetSite(), config.getDataCheckLevel(),
                    config.isQuickStart(), config.isRecycleSpace(), false);
        }

        /**
         * start file move task use ScmMoveTaskConfig. Async count the number of files,
         * when quick start is not enabled and the version of the server is v3.6
         *
         * @param config
         *            clean task config
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 3.6
         */
        public static ScmId startMoveTaskV2(ScmMoveTaskConfig config) throws ScmException {
            if (config == null) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startMoveTask(config.getWorkspace(), config.getCondition(), config.getScope(),
                    config.getMaxExecTime(), config.getTargetSite(), config.getDataCheckLevel(),
                    config.isQuickStart(), config.isRecycleSpace(), true);
        }

        private static ScmId _startMoveTask(ScmWorkspace ws, BSONObject condition, ScopeType scope,
                long maxExecTime, String targetSite, ScmDataCheckLevel dataCheckLevel,
                boolean quickStart, boolean isRecycleSpace, boolean isAsyncCountFile)
                throws ScmException {
            if (null == ws) {
                throw new ScmInvalidArgumentException("workspace is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }
            if (null == targetSite) {
                throw new ScmInvalidArgumentException("targetSite is null");
            }
            if (null == dataCheckLevel) {
                throw new ScmInvalidArgumentException("dataCheckLevel is null");
            }
            if (scope != ScopeType.SCOPE_CURRENT) {
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(condition);
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("invalid condition", e);
                }
            }
            ScmSession conn = ws.getSession();
            return conn.getDispatcher().MsgStartMoveTask(ws.getName(), condition, scope.getScope(),
                    maxExecTime, targetSite, dataCheckLevel.getName(), quickStart, isRecycleSpace,
                    isAsyncCountFile);
        }

        /**
         * start space recycling task use ScmSpaceRecyclingTaskConfig.
         *
         * @param config
         *            space recycling task config
         * @return task id
         * @throws ScmException
         *             If error happens
         * @since 3.1
         */
        public static ScmId startSpaceRecyclingTask(ScmSpaceRecyclingTaskConfig config)
                throws ScmException {
            if (config == null) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startSpaceRecyclingTask(config.getWorkspace(), config.getMaxExecTime(),
                    config.getRecycleScope());
        }

        private static ScmId _startSpaceRecyclingTask(ScmWorkspace workspace, long maxExecTime,
                ScmSpaceRecycleScope recycleScope) throws ScmException {

            if (null == workspace) {
                throw new ScmInvalidArgumentException("workspace is null");
            }
            if (null == recycleScope) {
                throw new ScmInvalidArgumentException("recycleScope is null");
            }
            ScmSession conn = workspace.getSession();
            return conn.getDispatcher().MsgStartSpaceRecyclingTask(workspace.getName(), maxExecTime,
                    recycleScope.getScope());
        }

        /**
         * stop task
         *
         * @param ss
         *            ScmSession
         * @param taskId
         *            task id
         * @throws ScmException
         *             If error happens
         * @since 2.1
         */
        public static void stopTask(ScmSession ss, ScmId taskId) throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (null == taskId) {
                throw new ScmInvalidArgumentException("taskId is null");
            }

            ss.getDispatcher().MsgStopTask(taskId);
        }

        /**
         * Get task by id
         *
         * @param ss
         *            ScmSession
         * @param taskId
         *            task id
         * @return task object
         * @throws ScmException
         *             If error happens
         * @since 2.1
         */
        public static ScmTask getTask(ScmSession ss, ScmId taskId) throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (null == taskId) {
                throw new ScmInvalidArgumentException("taskId is null");
            }

            BSONObject taskInfo = ss.getDispatcher().MsgGetTask(taskId);
            return new ScmTask(taskInfo, null, ss);
        }

        /**
         * Acquires task count which matches the query condition.
         *
         * @param ss
         *            session.
         * @param condition
         *            The condition of query task.
         * @throws ScmException
         *             If error happens
         * @since 3.1
         * @return
         */
        public static long count(ScmSession ss, BSONObject condition) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("condition", condition);
            return ss.getDispatcher().countTask(condition);
        }
    }

    /**
     * Provide Schedule operations.
     */
    public static class Schedule {
        private Schedule() {
            super();
        }

        /**
         * Create a schedule with specified args.
         *
         * @param ss
         *            session.
         * @param workspace
         *            workspace.
         * @param type
         *            schedule type.
         * @param name
         *            schedule name.
         * @param desc
         *            schedule description.
         * @param content
         *            schedule content.
         * @param cron
         *            schedule cron.
         * @return schedule instance.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmSchedule create(ScmSession ss, String workspace, ScheduleType type,
                String name, String desc, ScmScheduleContent content, String cron)
                throws ScmException {
            return create(ss, workspace, type, name, desc, content, cron, true);
        }

        /**
         * Create a schedule with specified args.
         *
         * @param ss
         *            session.
         * @param workspace
         *            workspace.
         * @param type
         *            schedule type.
         * @param name
         *            schedule name.
         * @param desc
         *            schedule description.
         * @param content
         *            schedule content.
         * @param cron
         *            schedule cron.
         * @param enable
         *            is enable the created schedule.
         * @return schedule instance.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmSchedule create(ScmSession ss, String workspace, ScheduleType type,
                String name, String desc, ScmScheduleContent content, String cron, boolean enable)
                throws ScmException {
            return new ScmScheduleBuilder(ss).workspace(workspace).name(name).content(content)
                    .cron(cron).description(desc).type(type).enable(enable).build();
        }

        /**
         * Create a schedule builder instance.
         * 
         * @param ss
         *            session.
         * @return schedule builder instance for create a schedule.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmScheduleBuilder scheduleBuilder(ScmSession ss) throws ScmException {
            return new ScmScheduleBuilder(ss);
        }

        private static void checkListParam(ScmSession ss, BSONObject condition)
                throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }
        }

        /**
         * List schedule.
         *
         * @param ss
         *            session.
         * @param condition
         *            the condition of query schedule, include:
         *            id,name,create_time,type,workspace,create_user,enable,cron,desc
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmScheduleBasicInfo> list(ScmSession ss, BSONObject condition)
                throws ScmException {
            return list(ss, condition, null, 0, -1);
        }

        /**
         * List schedule.
         *
         * @param ss
         *            session.
         * @param condition
         *            the condition of query schedule, include:
         *            id,name,create_time,type,workspace,create_user,enable,cron,desc
         * @param orderby
         *            the condition for sort, include:
         *            id,name,create_time,type,workspace,create_user,enable,cron,desc
         * @param skip
         *            skip the the specified amount of tasks, never skip if this
         *            parameter is 0.
         * @param limit
         *            return the specified amount of tasks, when limit is -1, return all
         *            the schedule.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmScheduleBasicInfo> list(ScmSession ss, BSONObject condition,
                BSONObject orderby, long skip, long limit) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("condition", condition);
            checkSkipAndLimit(skip, limit);
            BsonReader reader = ss.getDispatcher().getScheduleList(condition, orderby, skip, limit);
            ScmCursor<ScmScheduleBasicInfo> cursor = new ScmBsonCursor<ScmScheduleBasicInfo>(reader,
                    new BsonConverter<ScmScheduleBasicInfo>() {
                        @Override
                        public ScmScheduleBasicInfo convert(BSONObject obj) throws ScmException {
                            return new ScmScheduleBasicInfo(obj);
                        }
                    });
            return cursor;
        }

        private static void checkSkipAndLimit(long skip, long limit)
                throws ScmInvalidArgumentException {
            if (skip < 0) {
                throw new ScmInvalidArgumentException(
                        "skip must be greater than or equals to 0:skip=" + skip);
            }
            if (limit < -1) {
                throw new ScmInvalidArgumentException(
                        "limit must be greater than or equals to -1:limit=" + limit);
            }
        }

        private static void checkDeleteParam(ScmSession ss, ScmId scheduleId) throws ScmException {
            if (null == ss) {
                throw new ScmInvalidArgumentException("session is null");
            }

            if (null == scheduleId) {
                throw new ScmInvalidArgumentException("schedule id is null");
            }
        }

        /**
         * Delete specified schedule.
         *
         * @param ss
         *            session.
         * @param scheduleId
         *            schedule id.
         * @throws ScmException
         *             if error happens.
         */
        public static void delete(ScmSession ss, ScmId scheduleId) throws ScmException {
            checkDeleteParam(ss, scheduleId);
            ss.getDispatcher().deleteSchedule(scheduleId.get());
        }

        /**
         * Gets a schedule with specified id.
         *
         * @param ss
         *            session.
         * @param scheduleId
         *            id.
         * @return ScmSchedule.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmSchedule get(ScmSession ss, ScmId scheduleId) throws ScmException {
            checkDeleteParam(ss, scheduleId);
            BSONObject scheduleInfo = ss.getDispatcher().getSchedule(scheduleId.get());
            return new ScmScheduleImpl(ss, scheduleInfo);
        }

        /**
         * Acquires schedule count which matches the query condition.
         * 
         * @param ss
         *            session
         * @param condition
         *            The condition of query schedule.
         * @return long
         * @throws ScmException
         *             if error happens.
         * @since 3.1
         */
        public static long count(ScmSession ss, BSONObject condition) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("condition", condition);
            return ss.getDispatcher().countSchedule(condition);
        }
    }

    /**
     * Provide Scm process operations.
     *
     * @since 2.2
     */
    public static class ProcessInfo {
        private ProcessInfo() {
            super();
        }

        /**
         * Get Scm node process infomation.
         *
         * @param ss
         *            Session
         * @return ScmProcessInfo Process information.
         * @throws ScmException
         *             If error happens.
         * @since 2.2
         */
        public static ScmProcessInfo getProcessInfo(ScmSession ss) throws ScmException {
            List<String> keys = new ArrayList<String>();
            keys.add(PropertiesDefine.PROPERTY_SCM_VERSION);
            keys.add(PropertiesDefine.PROPERTY_SCM_REVISION);
            keys.add(PropertiesDefine.PROPERTY_SCM_COMPILE_TIME);
            keys.add(PropertiesDefine.PROPERTY_SCM_STATUS);
            keys.add(PropertiesDefine.PROPERTY_SCM_START_TIME);
            BSONObject bson = Configuration.getConfProperties(ss, keys);
            return new ScmProcessInfo(bson);
        }

    }

    /**
     * Provide Monitor operations.
     */
    public static class Monitor {

        private Monitor() {

        }

        /**
         * Lists health with specified service name.
         *
         * @param session
         *            session.
         * @param serviceName
         *            service name.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmHealth> listHealth(ScmSession session, String serviceName)
                throws ScmException {
            if (null == session) {
                throw new ScmInvalidArgumentException("session is null");
            }
            BsonReader reader = session.getDispatcher().listHealth(serviceName);
            ScmCursor<ScmHealth> cursor = new ScmBsonCursor<ScmHealth>(reader,
                    new BsonConverter<ScmHealth>() {
                        @Override
                        public ScmHealth convert(BSONObject obj) throws ScmException {
                            return new ScmHealth(obj);
                        }
                    });
            return cursor;
        }

        /**
         * Lists all host infomation.
         *
         * @param session
         *            session.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmHostInfo> listHostInfo(ScmSession session) throws ScmException {
            if (null == session) {
                throw new ScmInvalidArgumentException("session is null");
            }
            BsonReader reader = session.getDispatcher().listHostInfo();
            ScmCursor<ScmHostInfo> cursor = new ScmBsonCursor<ScmHostInfo>(reader,
                    new BsonConverter<ScmHostInfo>() {
                        @Override
                        public ScmHostInfo convert(BSONObject obj) throws ScmException {
                            return new ScmHostInfo(obj);
                        }
                    });
            return cursor;
        }

        /**
         * Shows all workspace flow.
         *
         * @param session
         *            session.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmFlow> showFlow(ScmSession session) throws ScmException {
            if (null == session) {
                throw new ScmInvalidArgumentException("session is null");
            }
            BsonReader reader = session.getDispatcher().showFlow();
            ScmCursor<ScmFlow> cursor = new ScmBsonCursor<ScmFlow>(reader,
                    new BsonConverter<ScmFlow>() {
                        @Override
                        public ScmFlow convert(BSONObject obj) throws ScmException {
                            return new ScmFlow(obj);
                        }
                    });
            return cursor;
        }

        /**
         * List gauge response.
         *
         * @param session
         *            session.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmGaugeResponse> gaugeResponse(ScmSession session)
                throws ScmException {
            if (null == session) {
                throw new ScmInvalidArgumentException("session is null");
            }
            BsonReader reader = session.getDispatcher().gaugeResponse();
            ScmCursor<ScmGaugeResponse> cursor = new ScmBsonCursor<ScmGaugeResponse>(reader,
                    new BsonConverter<ScmGaugeResponse>() {
                        @Override
                        public ScmGaugeResponse convert(BSONObject obj) throws ScmException {
                            return new ScmGaugeResponse(obj);
                        }
                    });
            return cursor;
        }
    }

    /**
     * Provide Schedule Statistics.
     */
    public static class Statistics {
        private Statistics() {
        }

        /**
         * Refresh the Statistics with specified args.
         *
         * @param ss
         *            session.
         * @param type
         *            statistics type.
         * @param workspace
         *            workspace name.
         * @throws ScmException
         *             if error happens.
         */
        public static void refresh(ScmSession ss, StatisticsType type, String workspace)
                throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("statistics type", type);
            checkArgNotNull("workspace", workspace);
            ss.getDispatcher().refreshStatistics(type, workspace);
        }

        /**
         * Refresh the object delta statistics with specified args.
         *
         * @param ss
         *            session.
         * @param bucketName
         *            bucket name.
         * @throws ScmException
         *             if error happens.
         */
        public static void refreshObjectDelta(ScmSession ss, String bucketName)
                throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("bucketName", bucketName);
            ss.getDispatcher().refreshObjectDeltaStatistics(bucketName);
        }

        /**
         * List files delta.
         *
         * @param ss
         *            session.
         * @param condition
         *            filter.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmStatisticsFileDelta> listFileDelta(ScmSession ss,
                BSONObject condition) throws ScmException {
            checkArgNotNull("session", ss);
            BsonReader reader = ss.getDispatcher().getStatisticsFileDeltaList(condition);
            ScmCursor<ScmStatisticsFileDelta> cursor = new ScmBsonCursor<ScmStatisticsFileDelta>(
                    reader, new BsonConverter<ScmStatisticsFileDelta>() {
                        @Override
                        public ScmStatisticsFileDelta convert(BSONObject obj) throws ScmException {
                            return new ScmStatisticsFileDelta(obj);
                        }
                    });
            return cursor;
        }

        /**
         * List object delta of bucket.
         *
         * @param ss
         *            session.
         * @param condition
         *            filter.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmStatisticsObjectDelta> listObjectDelta(ScmSession ss,
                BSONObject condition) throws ScmException {
            checkArgNotNull("session", ss);
            BsonReader reader = ss.getDispatcher().getStatisticsObjectDeltaList(condition);
            ScmCursor<ScmStatisticsObjectDelta> cursor = new ScmBsonCursor<ScmStatisticsObjectDelta>(
                    reader, new BsonConverter<ScmStatisticsObjectDelta>() {
                        @Override
                        public ScmStatisticsObjectDelta convert(BSONObject obj)
                                throws ScmException {
                            return new ScmStatisticsObjectDelta(obj);
                        }
                    });
            return cursor;
        }

        /**
         * List traffic with specified condition.
         *
         * @param ss
         *            session.
         * @param condition
         *            filter.
         * @return cursor.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmCursor<ScmStatisticsTraffic> listTraffic(ScmSession ss,
                BSONObject condition) throws ScmException {
            checkArgNotNull("session", ss);
            BsonReader reader = ss.getDispatcher().getStatisticsTrafficList(condition);
            ScmCursor<ScmStatisticsTraffic> cursor = new ScmBsonCursor<ScmStatisticsTraffic>(reader,
                    new BsonConverter<ScmStatisticsTraffic>() {
                        @Override
                        public ScmStatisticsTraffic convert(BSONObject obj) throws ScmException {
                            return new ScmStatisticsTraffic(obj);
                        }
                    });
            return cursor;
        }

        /**
         * Returns an file statistician for query statistics data.
         * 
         * @param session
         *            session.
         * @return statistician.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmFileStatistician fileStatistician(ScmSession session) throws ScmException {
            checkArgNotNull("session", session);
            return new ScmFileStatistician(session);
        }
    }

    /**
     * Provide Schedule ServiceCenter.
     */
    public static class ServiceCenter {
        private ServiceCenter() {
        }

        /**
         * Gets all service name from service center.
         *
         * @param ss
         *            Session.
         * @return List of service name.
         * @throws ScmException
         *             If error happens.
         */
        public static List<String> getServiceList(ScmSession ss) throws ScmException {
            List<ScmServiceInstance> instances = getServiceInstanceList(ss, null);
            List<String> ret = new ArrayList<String>();
            for (ScmServiceInstance instance : instances) {
                if (!ret.contains(instance.getServiceName())) {
                    ret.add(instance.getServiceName());
                }
            }
            return ret;
        }

        /**
         * Gets all site name from service center.
         *
         * @param ss
         *            Session.
         * @return List of site name.
         * @throws ScmException
         *             If error happens.
         */
        public static List<String> getSiteList(ScmSession ss) throws ScmException {
            List<ScmServiceInstance> instances = getServiceInstanceList(ss, null);
            List<String> ret = new ArrayList<String>();
            for (ScmServiceInstance instance : instances) {
                if (!ret.contains(instance.getServiceName()) && instance.isContentServer()) {
                    ret.add(instance.getServiceName());
                }
            }
            return ret;
        }

        /**
         * Gets all content server instance from service center.
         *
         * @param ss
         *            Session.
         * @return List of content server instance.
         * @throws ScmException
         *             If error happens.
         */
        public static List<ScmServiceInstance> getContentServerInstanceList(ScmSession ss)
                throws ScmException {
            List<ScmServiceInstance> instances = getServiceInstanceList(ss, null);
            List<ScmServiceInstance> ret = new ArrayList<ScmServiceInstance>();
            for (ScmServiceInstance instance : instances) {
                if (!ret.contains(instance) && instance.isContentServer()) {
                    ret.add(instance);
                }
            }
            return ret;
        }

        /**
         * Get service instances with specified service name.
         *
         * @param ss
         *            session.
         * @param serviceName
         *            sevice name.
         * @return service instances list.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmServiceInstance> getServiceInstanceList(ScmSession ss,
                String serviceName) throws ScmException {
            checkArgNotNull("session", ss);
            if (serviceName == null) {
                serviceName = "";
            }
            else {
                serviceName = serviceName.trim();
            }
            BSONObject resp = ss.getDispatcher().listServerInstance(serviceName);

            ArrayList<ScmServiceInstance> serverList = new ArrayList<ScmServiceInstance>();
            if (serviceName.equals("")) {
                BSONObject apps = BsonUtils.getBSONObjectChecked(resp, "applications");
                BasicBSONList appList = BsonUtils.getArrayChecked(apps, "application");
                for (Object app : appList) {
                    BSONObject appObj = (BSONObject) app;
                    formatAppObjBSON(serverList, appObj);
                }
            }
            else {
                BSONObject appObj = BsonUtils.getBSONObjectChecked(resp, "application");
                formatAppObjBSON(serverList, appObj);
            }
            return serverList;
        }

        private static void formatAppObjBSON(ArrayList<ScmServiceInstance> serverList,
                BSONObject appObj) throws ScmException {
            String serviceName = BsonUtils.getStringChecked(appObj, "name").toLowerCase();
            BasicBSONList instanceList = BsonUtils.getArrayChecked(appObj, "instance");
            for (Object instance : instanceList) {
                BSONObject instanceObj = (BSONObject) instance;
                String hostName = BsonUtils.getStringChecked(instanceObj, "hostName");
                String ip = BsonUtils.getStringChecked(instanceObj, "ipAddr");
                BSONObject portObj = BsonUtils.getBSONObjectChecked(instanceObj, "port");
                String status = BsonUtils.getString(instanceObj, "status");
                int port = BsonUtils.getIntegerChecked(portObj, "$");
                String vipAddress = BsonUtils.getString(instanceObj, "vipAddress");
                if (vipAddress != null && vipAddress.equalsIgnoreCase(serviceName)) {
                    serviceName = vipAddress;
                }
                BSONObject metaDataObj = BsonUtils.getBSONObjectChecked(instanceObj, "metadata");
                String zone = BsonUtils.getStringOrElse(metaDataObj, "zone",
                        ScmConfigOption.DEFAULT_ZONE);
                String region = BsonUtils.getStringOrElse(metaDataObj, "region",
                        ScmConfigOption.DEFAULT_REGION);
                boolean isContentServer = Boolean.valueOf(BsonUtils
                        .getObjectOrElse(metaDataObj, "isContentServer", "false").toString());
                boolean isRooSiteInstance = Boolean.valueOf(BsonUtils
                        .getObjectOrElse(metaDataObj, "isRootSiteInstance", "false").toString());
                serverList.add(new ScmServiceInstance(hostName, serviceName, region, zone, ip, port,
                        status, isContentServer, isRooSiteInstance, metaDataObj));
            }
        }
    }

    private static void checkArgNotNull(String argName, Object arg) throws ScmException {
        if (arg == null) {
            throw new ScmInvalidArgumentException(argName + " is null");
        }
    }

    /**
     * Provide service trace operations.
     *
     * @since 3.2
     */
    public static class ServiceTrace {

        /**
         * Get the trace list.
         * 
         * @param ss
         *            session.
         * @param limit
         *            max record count.
         * @return trace list.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmTrace> listTrace(ScmSession ss, int limit) throws ScmException {
            return listTrace(ss, null, null, null, null, null, limit);
        }

        /**
         * Get the trace list.
         * 
         * @param ss
         *            session.
         * @param startTime
         *            start time.
         * @param endTime
         *            end time.
         * @param limit
         *            max record count.
         * @return trace list.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmTrace> listTrace(ScmSession ss, Date startTime, Date endTime,
                int limit) throws ScmException {
            return listTrace(ss, null, null, startTime, endTime, null, limit);
        }

        /**
         * Get the trace list.
         *
         * @param ss
         *            session.
         * @param minDuration
         *            minimum duration time with microseconds.
         * @param limit
         *            max record count.
         * @return trace list.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmTrace> listTrace(ScmSession ss, Long minDuration, int limit)
                throws ScmException {
            return listTrace(ss, null, minDuration, null, null, null, limit);
        }

        /**
         * Get the trace list.
         *
         * @param ss
         *            session.
         * @param serviceName
         *            match trace containing the specified service name(if null, match
         *            all trace).
         * @param minDuration
         *            minimum duration time with microseconds.
         * @param startTime
         *            start time.
         * @param endTime
         *            end time.
         * @param queryCondition
         *            tag query condition.
         * @param limit
         *            max record count.
         * @return trace list.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmTrace> listTrace(ScmSession ss, String serviceName, Long minDuration,
                Date startTime, Date endTime, Map<String, String> queryCondition, int limit)
                throws ScmException {
            checkArgNotNull("session", ss);
            if (limit <= 0) {
                throw new ScmInvalidArgumentException(
                        "limit must be greater than 0: limit=" + limit);
            }
            if (minDuration != null && minDuration <= 0) {
                throw new ScmInvalidArgumentException(
                        "minDuration must be greater than 0: minDuration=" + minDuration);
            }
            if (startTime != null && endTime != null && endTime.getTime() < startTime.getTime()) {
                throw new ScmInvalidArgumentException(
                        "endTime must be greater than or equal to startTime: startTime="
                                + startTime.getTime() + " ,endTime=" + endTime.getTime());
            }
            if (queryCondition != null && queryCondition.containsKey(null)) {
                throw new ScmInvalidArgumentException(
                        "queryCondition cannot contains null key:queryCondition=" + queryCondition);
            }
            BasicBSONList traceList = (BasicBSONList) ss.getDispatcher().listTrace(minDuration,
                    serviceName, startTime, endTime, queryCondition, limit);
            List<ScmTrace> scmTraceList = new ArrayList<ScmTrace>();
            for (Object o : traceList) {
                BasicBSONList spanBsonList = (BasicBSONList) o;
                scmTraceList.add(new ScmTrace(spanBsonList));
            }
            return scmTraceList;
        }

        /**
         * Get trace by trace id.
         * 
         * @param ss
         *            session.
         * @param traceId
         *            trace id.
         * @return trace info.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmTrace getTrace(ScmSession ss, String traceId) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("traceId", traceId);
            try {
                BasicBSONList traceSpanList = (BasicBSONList) ss.getDispatcher().getTrace(traceId);
                return new ScmTrace(traceSpanList);
            }
            catch (ScmException e) {
                if (e.getError() == ScmError.HTTP_NOT_FOUND) {
                    throw new ScmInvalidArgumentException("traceId is not exist:traceId=" + traceId,
                            e);
                }
                throw e;
            }
        }

    }

    /**
     * Provide life cycle config operations.
     *
     * @since 3.2
     */
    public static class LifeCycleConfig {

        /**
         * start once transition use ScmOnceTransitionConfig.
         *
         * @param config
         *            once transition config.
         * 
         * @return task ScmId.
         * @throws ScmException
         *             if error happens.
         * @since v3.2
         * @deprecated Starting from v3.6, disuse this interface, please use
         *             startOnceTransitionV2 interface.
         */
        public static ScmId startOnceTransition(ScmOnceTransitionConfig config)
                throws ScmException {
            if (config == null) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startOnceTransition(config.getWorkspace(), config.getCondition(),
                    config.getScope(), config.getMaxExecTime(), config.getSourceStageTag(),
                    config.getDestStageTag(), config.getDataCheckLevel(), config.isQuickStart(),
                    config.isRecycleSpace(), config.getType(), config.getPreferredRegion(),
                    config.getPreferredZone(), false);
        }

        /**
         * start once transition use ScmOnceTransitionConfig.Async count the number of
         * files, when quick start is not enabled and the version of the server is v3.6
         *
         * @param config
         *            once transition config.
         *
         * @return task ScmId.
         * @throws ScmException
         *             if error happens.
         * @since v3.6
         */
        public static ScmId startOnceTransitionV2(ScmOnceTransitionConfig config)
                throws ScmException {
            if (config == null) {
                throw new ScmInvalidArgumentException("config is null");
            }
            return _startOnceTransition(config.getWorkspace(), config.getCondition(),
                    config.getScope(), config.getMaxExecTime(), config.getSourceStageTag(),
                    config.getDestStageTag(), config.getDataCheckLevel(), config.isQuickStart(),
                    config.isRecycleSpace(), config.getType(), config.getPreferredRegion(),
                    config.getPreferredZone(), true);
        }

        private static ScmId _startOnceTransition(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, long maxExecTime, String source, String dest,
                ScmDataCheckLevel dataCheckLevel, boolean quickStart, boolean isRecycleSpace,
                String type, String preferredRegion, String preferredZone, boolean isAsyncCountFile)
                throws ScmException {
            if (null == ws) {
                throw new ScmInvalidArgumentException("workspace is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }
            if (null == source) {
                throw new ScmInvalidArgumentException("source stage tag is null");
            }
            if (null == dest) {
                throw new ScmInvalidArgumentException("dest stage tag is null");
            }
            if (null == dataCheckLevel) {
                throw new ScmInvalidArgumentException("dataCheckLevel is null");
            }
            if (null == type) {
                throw new ScmInvalidArgumentException("type is null");
            }
            else {
                if (ScheduleType.getType(type) != ScheduleType.MOVE_FILE
                        && ScheduleType.getType(type) != ScheduleType.COPY_FILE) {
                    throw new ScmInvalidArgumentException(
                            "unsupported this task type,type=" + type);
                }
            }
            if (null == preferredRegion) {
                throw new ScmInvalidArgumentException("preferredRegion is null");
            }
            if (null == preferredZone) {
                throw new ScmInvalidArgumentException("preferredZone is null");
            }
            if (scope != ScopeType.SCOPE_CURRENT) {
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(condition);
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("invalid condition", e);
                }
            }
            ScmSession conn = ws.getSession();
            return conn.getDispatcher().startOnceTransition(ws.getName(), condition,
                    scope.getScope(), maxExecTime, source, dest, dataCheckLevel.getName(),
                    quickStart, isRecycleSpace, type, preferredRegion, preferredZone,
                    isAsyncCountFile);
        }

        /**
         * get transition list by stage tag name.
         *
         * @param stageTagName
         *            stage tag name.
         * 
         * @param ss
         *            session.
         *
         * @return ScmLifeCycleTransitionList.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmLifeCycleTransition> listTransition(ScmSession ss,
                String stageTagName) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("stageTagName", stageTagName);

            BasicBSONList list = (BasicBSONList) ss.getDispatcher().listTransition(stageTagName);
            List<ScmLifeCycleTransition> transitionList = new ArrayList<ScmLifeCycleTransition>();
            for (Object o : list) {
                transitionList.add(ScmLifeCycleTransition.fromRecord((BSONObject) o));
            }
            return transitionList;
        }

        /**
         * get apply transition workspace by transition name.
         *
         * @param transitionName
         *            transition name.
         *
         * @param ss
         *            session.
         *
         * @return ScmTransitionApplyInfo.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmTransitionApplyInfo> listWorkspace(ScmSession ss,
                String transitionName) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("transitionName", transitionName);
            BSONObject obj = ss.getDispatcher().listWorkspaceByTransitionName(transitionName);
            List<ScmTransitionApplyInfo> result = new ArrayList<ScmTransitionApplyInfo>();
            BasicBSONList customized = BsonUtils.getArray(obj, "customized");
            BasicBSONList uncustomized = BsonUtils.getArray(obj, "uncustomized");
            for (Object o : customized) {
                result.add(new ScmTransitionApplyInfo((String) o, true));
            }
            for (Object o : uncustomized) {
                result.add(new ScmTransitionApplyInfo((String) o, false));
            }
            return result;
        }

        /**
         * get all transition.
         *
         * @param ss
         *            session.
         *
         * @return ScmLifeCycleTransitionList.
         * @throws ScmException
         *             if error happens.
         */
        public static List<ScmLifeCycleTransition> getTransitionConfig(ScmSession ss)
                throws ScmException {
            checkArgNotNull("session", ss);
            BasicBSONList list = (BasicBSONList) ss.getDispatcher().listTransition(null);
            List<ScmLifeCycleTransition> transitionList = new ArrayList<ScmLifeCycleTransition>();
            for (Object o : list) {
                transitionList.add(ScmLifeCycleTransition.fromRecord((BSONObject) o));
            }
            return transitionList;
        }

        /**
         * get all stage tag
         * 
         * @param ss
         *            session
         * @return ScmLifeCycleStageTagList
         * @throws ScmException
         *             if error happens
         */
        public static List<ScmLifeCycleStageTag> getStageTag(ScmSession ss) throws ScmException {
            checkArgNotNull("session", ss);
            BasicBSONList list = (BasicBSONList) ss.getDispatcher().listStageTag();
            List<ScmLifeCycleStageTag> stageTagList = new ArrayList<ScmLifeCycleStageTag>();
            for (Object o : list) {
                String name = BsonUtils.getString((BSONObject) o, "name");
                String desc = BsonUtils.getString((BSONObject) o, "desc");
                stageTagList.add(new ScmLifeCycleStageTag(name, desc));
            }
            return stageTagList;
        }

        /**
         * update transition by transition name.
         *
         * @param transitionName
         *            transition name.
         * @param transition
         *            new transition info
         *
         * @param ss
         *            session.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void updateTransition(ScmSession ss, String transitionName,
                ScmLifeCycleTransition transition) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("transitionName", transitionName);
            checkTransitionValid(transition);
            ss.getDispatcher().updateGlobalTransition(transitionName, transition.toBSONObject());
        }

        /**
         * get transition by transition name.
         *
         * @param ss
         *            session.
         *
         * @param transitionName
         *            transition name
         *
         * @return ScmLifeCycleTransition.
         * @throws ScmException
         *             if error happens.
         */
        public static ScmLifeCycleTransition getTransition(ScmSession ss, String transitionName)
                throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("transitionName", transitionName);
            BSONObject obj = ss.getDispatcher().getGlobalTransition(transitionName);
            return ScmLifeCycleTransition.fromRecord(obj);
        }

        /**
         * remove transition.
         *
         * @param ss
         *            session.
         *
         * @param transitionName
         *            transition name.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void removeTransition(ScmSession ss, String transitionName)
                throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("transitionName", transitionName);
            ss.getDispatcher().removeGlobalTransition(transitionName);
        }

        /**
         * add transition.
         *
         * @param ss
         *            session.
         *
         * @param transition
         *            transition info.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void addTransition(ScmSession ss, ScmLifeCycleTransition transition)
                throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("transition", transition);
            checkTransitionValid(transition);
            ss.getDispatcher().addGlobalTransition(transition.toBSONObject());
        }

        /**
         * remove stage tag by stage tag name.
         *
         * @param ss
         *            session.
         *
         * @param stageTagName
         *            stage tag name.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void removeStageTag(ScmSession ss, String stageTagName) throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("stageName", stageTagName);
            ss.getDispatcher().removeGlobalStageTag(stageTagName);
        }

        /**
         * add stage tag.
         *
         * @param ss
         *            session.
         *
         * @param stageTagName
         *            stage tag name.
         *
         * @param stageTagDesc
         *            stage tag desc.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void addStageTag(ScmSession ss, String stageTagName, String stageTagDesc)
                throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("stageName", stageTagName);
            ss.getDispatcher().addGlobalStageTag(stageTagName, stageTagDesc);
        }

        /**
         * delete life cycle config.
         *
         * @param ss
         *            session.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void deleteLifeCycleConfig(ScmSession ss) throws ScmException {
            checkArgNotNull("session", ss);
            ss.getDispatcher().deleteGlobalLifeCycleConfig();
        }

        /**
         * get life cycle config.
         *
         * @param ss
         *            session.
         *
         * @return ScmLifeCycleConfig
         * @throws ScmException
         *             if error happens.
         */
        public static ScmLifeCycleConfig getLifeCycleConfig(ScmSession ss) throws ScmException {
            checkArgNotNull("session", ss);
            BSONObject obj = ss.getDispatcher().getGlobalLifeCycleConfig();
            return ScmLifeCycleConfig.fromRecord(obj);
        }

        /**
         * set life cycle config by xml config file path.
         *
         * @param ss
         *            session.
         *
         * @param xmlConfigFilePath
         *            xml config file path.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void setLifeCycleConfig(ScmSession ss, String xmlConfigFilePath)
                throws ScmException {
            InputStream in = null;
            try {
                in = new FileInputStream(xmlConfigFilePath);
                setLifeCycleConfig(ss, in);
            }
            catch (IOException e) {
                throw new ScmException(ScmError.FILE_IO,
                        "failed to read the xml file,xmlPath=" + xmlConfigFilePath, e);
            }
            finally {
                IOUtils.close(in);
            }
        }

        public static void setLifeCycleConfig(ScmSession ss, InputStream xmlInputStream)
                throws ScmException {
            ScmLifeCycleConfig lifeCycleConfig = null;
            try {
                BSONObject obj = XMLUtils.xmlToBSONObj(xmlInputStream);
                if (obj != null) {
                    lifeCycleConfig = ScmLifeCycleConfig.fromUser(obj);
                }
            }
            catch (Exception e) {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "unable to parse xml life cycle config content", e);
            }
            setLifeCycleConfig(ss, lifeCycleConfig);
        }

        /**
         * set life cycle config by ScmLifeCycleConfig.
         *
         * @param ss
         *            session.
         *
         * @param lifeCycleConfig
         *            ScmLifeCycleConfig.
         *
         * @throws ScmException
         *             if error happens.
         */
        public static void setLifeCycleConfig(ScmSession ss, ScmLifeCycleConfig lifeCycleConfig)
                throws ScmException {
            checkArgNotNull("session", ss);
            checkArgNotNull("life cycle config", lifeCycleConfig);
            checkLifeCycleConfigValid(lifeCycleConfig);
            ss.getDispatcher().setGlobalLifeCycleConfig(lifeCycleConfig.toBSONObject());
        }

        private static void checkLifeCycleConfigValid(ScmLifeCycleConfig lifeCycleConfig)
                throws ScmException {
            checkStageTagConfigValid(lifeCycleConfig.getStageTagConfig());
            checkTransitionConfigValid(lifeCycleConfig.getTransitionConfig());
        }

        private static void checkStageTagConfigValid(List<ScmLifeCycleStageTag> stageTagConfig)
                throws ScmException {
            checkArgNotNull("stage tag configuration", stageTagConfig);
            for (ScmLifeCycleStageTag stageTag : stageTagConfig) {
                checkStringArgValid("stage tag name", stageTag.getName());
            }

        }

        private static void checkTransitionConfigValid(
                List<ScmLifeCycleTransition> transitionConfig) throws ScmException {
            checkArgNotNull("transition configuration", transitionConfig);
            for (ScmLifeCycleTransition transition : transitionConfig) {
                checkTransitionValid(transition);
            }
        }

        private static void checkTransitionValid(ScmLifeCycleTransition transition)
                throws ScmException {
            checkArgNotNull("transition", transition);
            checkStringArgValid("transition name", transition.getName());
            checkFlowValid(transition.getSource(), transition.getDest());
            checkTransitionTriggersValid(transition.getTransitionTriggers());
            if (transition.getCleanTriggers() != null && !transition.getCleanTriggers().isEmpty()) {
                checkCleanTriggersValid(transition.getCleanTriggers());
            }
            checkExtraContentValid(transition.getDataCheckLevel(), transition.getScope());
        }

        private static void checkExtraContentValid(String dataCheckLevel, String scope)
                throws ScmInvalidArgumentException {
            checkStringArgValid("dataCheckLevel", dataCheckLevel);
            checkStringArgValid("scope", scope);
            ScmDataCheckLevel type = ScmDataCheckLevel.getType(dataCheckLevel.toLowerCase());
            if (type == ScmDataCheckLevel.UNKNOWN) {
                throw new ScmInvalidArgumentException(
                        "the dataCheckLevel only choose strict or week,dataCheckLevel="
                                + dataCheckLevel);
            }
            if (!scope.equals("ALL") && !scope.equals("CURRENT") && !scope.equals("HISTORY")) {
                throw new ScmInvalidArgumentException(
                        "the scope only choose ALL or CURRENT or HISTORY.scope=" + scope);
            }
        }

        private static void checkCleanTriggersValid(ScmCleanTriggers cleanTriggers)
                throws ScmException {
            checkArgNotNull("clean triggers", cleanTriggers);
            checkStringArgValid("clean triggers mode", cleanTriggers.getMode());
            checkStringArgValid("clean triggers rule", cleanTriggers.getRule());
            checkArgNotNull("clean trigger list", cleanTriggers.getTriggerList());
            if (cleanTriggers.getMaxExecTime() < 0) {
                throw new ScmInvalidArgumentException(
                        "clean triggers maxExecTime must be greater than 0");
            }
            if (cleanTriggers.getTriggerList().size() == 0) {
                throw new ScmInvalidArgumentException(
                        "clean triggers trigger list should not be empty");
            }
            for (ScmTrigger trigger : cleanTriggers.getTriggerList()) {
                checkTriggerValid("CleanTriggers", trigger);
            }
        }

        private static void checkTransitionTriggersValid(ScmTransitionTriggers transitionTriggers)
                throws ScmException {
            checkArgNotNull("transition triggers", transitionTriggers);
            checkStringArgValid("transition triggers mode", transitionTriggers.getMode());
            checkStringArgValid("transition triggers rule", transitionTriggers.getRule());
            checkArgNotNull("transition trigger list", transitionTriggers.getTriggerList());
            if (transitionTriggers.getMaxExecTime() < 0) {
                throw new ScmInvalidArgumentException(
                        "transition triggers maxExecTime must be greater than 0");
            }
            if (transitionTriggers.getTriggerList().size() == 0) {
                throw new ScmInvalidArgumentException(
                        "transition triggers trigger list should not be empty");
            }
            for (ScmTrigger trigger : transitionTriggers.getTriggerList()) {
                checkTriggerValid("TransitionTriggers", trigger);
            }
        }

        private static void checkTriggerValid(String checkType, ScmTrigger trigger)
                throws ScmException {
            checkArgNotNull("trigger", trigger);
            checkStringArgValid("trigger ID", trigger.getID());
            checkStringArgValid("triggers mode", trigger.getMode());
            checkStringArgValid("trigger lastAccessTime", trigger.getLastAccessTime());
            if (checkType.equals("TransitionTriggers")) {
                checkStringArgValid("trigger createTime", trigger.getCreateTime());
                checkStringArgValid("trigger buildTime", trigger.getBuildTime());

            }
            else if (checkType.equals("CleanTriggers")) {
                checkStringArgValid("trigger transitionTime", trigger.getTransitionTime());
            }
        }

        private static void checkFlowValid(String source, String dest) throws ScmException {
            checkStringArgValid("flow source", source);
            checkStringArgValid("flow dest", dest);
            if (source.equals(dest)) {
                throw new ScmInvalidArgumentException("flow source cannot be the same");
            }
        }

        private static void checkStringArgValid(String argName, String argValue)
                throws ScmInvalidArgumentException {
            if (!Strings.hasText(argValue)) {
                throw new ScmInvalidArgumentException("invalid " + argName + "=" + argValue);
            }
        }
    }
}
