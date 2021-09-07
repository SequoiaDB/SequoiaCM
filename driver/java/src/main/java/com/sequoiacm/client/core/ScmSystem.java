package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.StatisticsType;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.common.ScmArgChecker;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide ScmSystem operations.
 *
 * @since 2.1
 */
public class ScmSystem {

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
         */
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
         */
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
         */
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition)
                throws ScmException {
            return startTransferTask(ws, condition, ScopeType.SCOPE_CURRENT, null);

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
         */
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope) throws ScmException {
            return startTransferTask(ws, condition, scope, 0);
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
         */
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, long maxExecTime) throws ScmException {
            return startTransferTask(ws, condition, scope, maxExecTime, null);
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
         */
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, String targetSite) throws ScmException {
            return startTransferTask(ws, condition, scope, 0, targetSite);
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
         */
        public static ScmId startTransferTask(ScmWorkspace ws, BSONObject condition,
                ScopeType scope, long maxExecTime, String targetSite) throws ScmException {
            if (null == ws) {
                throw new ScmInvalidArgumentException("workspace is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }
            if (scope != ScopeType.SCOPE_CURRENT) {
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(condition);
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("invlid condition", e);
                }
            }

            ScmSession conn = ws.getSession();
            return conn.getDispatcher().MsgStartTransferTask(ws.getName(), condition,
                    scope.getScope(), maxExecTime, targetSite);

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
         */
        public static ScmId startCleanTask(ScmWorkspace ws, BSONObject condition)
                throws ScmException {
            return startCleanTask(ws, condition, ScopeType.SCOPE_CURRENT);
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
         */
        public static ScmId startCleanTask(ScmWorkspace ws, BSONObject condition, ScopeType scope)
                throws ScmException {
            return startCleanTask(ws, condition, scope, 0);
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
         */
        public static ScmId startCleanTask(ScmWorkspace ws, BSONObject condition, ScopeType scope,
                long maxExecTime) throws ScmException {
            if (null == ws) {
                throw new ScmInvalidArgumentException("workspace is null");
            }

            if (null == condition) {
                throw new ScmInvalidArgumentException("condition is null");
            }

            if (null == scope) {
                throw new ScmInvalidArgumentException("scope is null");
            }
            if (scope != ScopeType.SCOPE_CURRENT) {
                try {
                    ScmArgChecker.File.checkHistoryFileMatcher(condition);
                }
                catch (InvalidArgumentException e) {
                    throw new ScmInvalidArgumentException("invlid condition", e);
                }
            }
            ScmSession conn = ws.getSession();
            return conn.getDispatcher().MsgStartCleanTask(ws.getName(), condition, scope.getScope(),
                    maxExecTime);
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
            return new ScmTask(taskInfo);
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
            checkArgNotNull("session",ss);
            checkArgNotNull("condition",condition);
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
         * @param ss session.
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

        private static void checkSkipAndLimit(long skip, long limit) throws ScmInvalidArgumentException {
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
         * @param ss
         *          session
         * @param condition
         *          The condition of query schedule.
         * @return long
         * @throws ScmException
         *          if error happens.
         * @since 3.1
         */
        public static long count(ScmSession ss, BSONObject condition) throws ScmException {
            checkArgNotNull("session",ss);
            checkArgNotNull("condition",condition);
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
                serverList.add(new ScmServiceInstance(serviceName, region, zone, ip, port, status,
                        isContentServer, isRooSiteInstance));
            }
        }
    }

    private static void checkArgNotNull(String argName, Object arg) throws ScmException {
        if (arg == null) {
            throw new ScmInvalidArgumentException(argName + " is null");
        }
    }
}
