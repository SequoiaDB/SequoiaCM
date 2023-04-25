package com.sequoiacm.metasource.sequoiadb.accessor;

import java.util.Date;

import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaTaskAccessor;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiadb.exception.SDBError;

public class SdbTaskAccessor extends SdbMetaAccessor implements MetaTaskAccessor {
    private static final Logger logger = LoggerFactory.getLogger(SdbTaskAccessor.class);

    public SdbTaskAccessor(SdbMetaSource metasource, String csName, String clName) {
        super(metasource, csName, clName);
    }

    @Override
    public void delete(String taskId) throws SdbMetasourceException {
        try {
            BSONObject deletor = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            delete(deletor);
        }
        catch (SdbMetasourceException e) {
            logger.error("delete task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "delete task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId, e);
        }
    }

    @Override
    public void abortAllTask(int serverId, Date stopTime) throws SdbMetasourceException {
        try {
            Date date = new Date();
            BSONObject flagList = new BasicBSONList();
            flagList.put("0", CommonDefine.TaskRunningFlag.SCM_TASK_INIT);
            flagList.put("1", CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING);
            BSONObject inFlag = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MATCHER_IN, flagList);

            BSONObject typeList = new BasicBSONList();
            typeList.put("0", CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);
            typeList.put("1", CommonDefine.TaskType.SCM_TASK_CLEAN_FILE);
            typeList.put("2", CommonDefine.TaskType.SCM_TASK_MOVE_FILE);
            typeList.put("3", CommonDefine.TaskType.SCM_TASK_RECYCLE_SPACE);
            BSONObject inType = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MATCHER_IN, typeList);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.Task.FIELD_RUNNING_FLAG, inFlag);
            matcher.put(FieldName.Task.FIELD_TYPE, inType);
            matcher.put(FieldName.Task.FIELD_SERVER_ID, serverId);

            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Task.FIELD_RUNNING_FLAG,
                    CommonDefine.TaskRunningFlag.SCM_TASK_ABORT);
            newValue.put(FieldName.Task.FIELD_STOP_TIME, date.getTime());

            BSONObject updator = new BasicBSONObject();
            updator.put(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, newValue);
            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("abort task failed:table=" + getCsName() + "." + getClName()
                    + ",serverId=" + serverId + ",date=" + stopTime);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "abort task failed:table=" + getCsName() + "." + getClName()
                    + ",serverId=" + serverId + ",date=" + stopTime, e);
        }
    }

    @Override
    public void setAllStopTime(int serverId, Date stopTime) throws SdbMetasourceException {
        try {
            BSONObject typeList = new BasicBSONList();
            typeList.put("0", CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);
            typeList.put("1", CommonDefine.TaskType.SCM_TASK_CLEAN_FILE);
            typeList.put("2", CommonDefine.TaskType.SCM_TASK_MOVE_FILE);
            typeList.put("3", CommonDefine.TaskType.SCM_TASK_RECYCLE_SPACE);
            BSONObject inType = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MATCHER_IN, typeList);

            BSONObject isNull = new BasicBSONObject();
            isNull.put(SequoiadbHelper.SEQUOIADB_MATCHER_ISNULL, 1);

            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.Task.FIELD_TYPE, inType);
            matcher.put(FieldName.Task.FIELD_SERVER_ID, serverId);
            matcher.put(FieldName.Task.FIELD_STOP_TIME, isNull);

            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Task.FIELD_STOP_TIME, stopTime.getTime());

            BSONObject updator = new BasicBSONObject();
            updator.put(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, newValue);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("set stop time failed:table=" + getCsName() + "." + getClName()
                    + ",serverId=" + serverId + ",date=" + stopTime);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "set stop time failed:table=" + getCsName() + "." + getClName()
                    + ",serverId=" + serverId + ",date=" + stopTime, e);
        }
    }

    @Override
    public void abort(String taskId, int flag, String detail, Date stopTime, int progress,
            long successCount,
            long failedCount) throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);

            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Task.FIELD_RUNNING_FLAG, flag);
            newValue.put(FieldName.Task.FIELD_DETAIL, detail);
            newValue.put(FieldName.Task.FIELD_STOP_TIME, stopTime.getTime());
            newValue.put(FieldName.Task.FIELD_PROGRESS, progress);
            newValue.put(FieldName.Task.FIELD_SUCCESS_COUNT, successCount);
            newValue.put(FieldName.Task.FIELD_FAIL_COUNT, failedCount);

            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    newValue);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("abort task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId + ",detail=" + detail);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "abort task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId + ",detail=" + detail, e);
        }
    }

    @Override
    public void finish(String taskId, Date stopTime, long successCount,
            long failedCount) throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);

            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Task.FIELD_RUNNING_FLAG,
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
            newValue.put(FieldName.Task.FIELD_STOP_TIME, stopTime.getTime());
            newValue.put(FieldName.Task.FIELD_PROGRESS, 100);
            newValue.put(FieldName.Task.FIELD_SUCCESS_COUNT, successCount);
            newValue.put(FieldName.Task.FIELD_FAIL_COUNT, failedCount);
            newValue.put(FieldName.Task.FIELD_ACTUAL_COUNT, successCount + failedCount);
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    newValue);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("finish task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "finish task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId, e);
        }
    }

    @Override
    public void updateStopTimeIfEmpty(String taskId, Date stopTime, int progress, long successCount,
            long failedCount) throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject isNull = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MATCHER_ISNULL, 1);
            matcher.put(FieldName.Task.FIELD_STOP_TIME, isNull);

            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Task.FIELD_STOP_TIME, stopTime.getTime());
            newValue.put(FieldName.Task.FIELD_PROGRESS, progress);
            newValue.put(FieldName.Task.FIELD_SUCCESS_COUNT, successCount);
            newValue.put(FieldName.Task.FIELD_FAIL_COUNT, failedCount);

            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    newValue);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("update stop time failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update stop time failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId, e);
        }
    }

    @Override
    public void updateExtraInfo(String taskId, BSONObject extraInfo) throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject bson = new BasicBSONObject(FieldName.Task.FIELD_EXTRA_INFO, extraInfo);
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, bson);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("update extra info failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update extra info failed:table=" + getCsName() + "." + getClName() + ",taskId="
                            + taskId,
                    e);
        }
    }

    @Override
    public void updateActualAndEstimateCount(String taskId, long actualCount, long estimateCount)
            throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Task.FIELD_ESTIMATE_COUNT, estimateCount);
            newValue.put(FieldName.Task.FIELD_ACTUAL_COUNT, actualCount);

            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    newValue);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("update actualCount and estimateCount failed:table=" + getCsName() + "."
                    + getClName() + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update actualCount and estimateCount failed:table=" + getCsName() + "."
                            + getClName() + ",taskId=" + taskId,
                    e);
        }
    }

    @Override
    public void cancel(String taskId, String detail) throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject flagList = new BasicBSONList();
            flagList.put("0", CommonDefine.TaskRunningFlag.SCM_TASK_INIT);
            flagList.put("1", CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING);
            BSONObject inFlag = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MATCHER_IN, flagList);
            matcher.put(FieldName.Task.FIELD_RUNNING_FLAG, inFlag);

            BSONObject newValue = new BasicBSONObject(FieldName.Task.FIELD_RUNNING_FLAG,
                    CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL);
            newValue.put(FieldName.Task.FIELD_DETAIL, detail);
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET, newValue);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("cancel task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "cancel task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId, e);
        }
    }

    @Override
    public boolean checkAndStartTask(String taskId, Date startTime, long estimateCount, long actualCount)
            throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            matcher.put(FieldName.Task.FIELD_RUNNING_FLAG, CommonDefine.TaskRunningFlag.SCM_TASK_INIT);

            BSONObject newValue = new BasicBSONObject(FieldName.Task.FIELD_RUNNING_FLAG,
                    CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING);
            newValue.put(FieldName.Task.FIELD_START_TIME, startTime.getTime());

            newValue.put(FieldName.Task.FIELD_ESTIMATE_COUNT, estimateCount);
            newValue.put(FieldName.Task.FIELD_ACTUAL_COUNT, actualCount);

            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    newValue);
            BSONObject bsonObject = queryAndUpdate(matcher, updator, null);
            if (bsonObject != null) {
                return true;
            }
            return false;
        }
        catch (SdbMetasourceException e) {
            logger.error("start task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "start task failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId, e);
        }
    }

    @Override
    public void updateTaskFileCount(String taskId, long estimateCount, long actualCount)
            throws ScmMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject newValue = new BasicBSONObject();
            newValue.put(FieldName.Task.FIELD_ESTIMATE_COUNT, estimateCount);
            newValue.put(FieldName.Task.FIELD_ACTUAL_COUNT, actualCount);
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    newValue);
            update(matcher, updator);
        }
        catch (ScmMetasourceException e) {
            logger.error("update task file count failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update task file count failed:table=" + getCsName() + "." + getClName()
                            + ",taskId=" + taskId,
                    e);
        }
    }

    @Override
    public void updateProgress(String taskId, int progress, long successCount, long failedCount)
            throws SdbMetasourceException {
        try {
            BSONObject matcher = new BasicBSONObject(FieldName.Task.FIELD_ID, taskId);
            BSONObject bsonProgress = new BasicBSONObject(FieldName.Task.FIELD_PROGRESS,
                    progress);
            bsonProgress.put(FieldName.Task.FIELD_SUCCESS_COUNT, successCount);
            bsonProgress.put(FieldName.Task.FIELD_FAIL_COUNT, failedCount);
            BSONObject updator = new BasicBSONObject(SequoiadbHelper.SEQUOIADB_MODIFIER_SET,
                    bsonProgress);

            update(matcher, updator);
        }
        catch (SdbMetasourceException e) {
            logger.error("update progress failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId);
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "update progress failed:table=" + getCsName() + "." + getClName()
                    + ",taskId=" + taskId, e);
        }
    }

}