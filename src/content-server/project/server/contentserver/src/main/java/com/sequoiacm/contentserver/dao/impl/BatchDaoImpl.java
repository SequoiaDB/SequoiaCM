package com.sequoiacm.contentserver.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.IBatchDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.metasource.BatchMetaCursorFillInFileCount;
import com.sequoiacm.metasource.MetaCursor;

@Repository
public class BatchDaoImpl implements IBatchDao {
    @Autowired
    private FileOperationListenerMgr listenerMgr;
    private static final Logger logger = LoggerFactory.getLogger(BatchDaoImpl.class);

    @Override
    public String insert(ScmWorkspaceInfo wsInfo, BSONObject batchInfo, String userName)
            throws ScmServerException {
        String batchId = null;
        try {
            batchInfo = checkCreateObj(batchInfo);
            batchId = addExtraFields(wsInfo, batchInfo, userName);

            ScmContentModule.getInstance().getMetaService().insertBatch(wsInfo, batchInfo);
        }
        catch (Exception e) {
            logger.error("create batch failed: workspace={}, batchId={}", wsInfo.getName(),
                    batchId);
            throw e;
        }
        return batchId;
    }

    @Override
    public void delete(ScmWorkspaceInfo wsInfo, String batchId, String batchCreateMonth,
            String sessionId, String userDetail, String user) throws ScmServerException {
        try {
            ScmContentModule.getInstance().getMetaService().deleteBatch(wsInfo, batchId,
                    batchCreateMonth, sessionId, userDetail, user, listenerMgr);
        }
        catch (ScmServerException e) {
            logger.error("delete batch failed: workspace={}, batchId={}", wsInfo.getName(),
                    batchId);
            throw e;
        }
    }

    @Override
    public BSONObject queryById(ScmWorkspaceInfo wsInfo, String batchId, String batchCreateMonth)
            throws ScmServerException {
        BSONObject batchInfo = null;
        try {
            batchInfo = ScmContentModule.getInstance().getMetaService().getBatchInfo(wsInfo,
                    batchId, batchCreateMonth);
        }
        catch (ScmServerException e) {
            logger.error("queryById failed: workspace={}, batchId={}", wsInfo.getName(), batchId);
            throw e;
        }
        return batchInfo;
    }

    @Override
    public MetaCursor query(ScmWorkspaceInfo wsInfo, BSONObject matcher, BSONObject orderBy,
            long skip, long limit) throws ScmServerException {
        MetaCursor cursor = null;
        try {
            BSONObject selector = new BasicBSONObject();
            selector.put(FieldName.Batch.FIELD_ID, null);
            selector.put(FieldName.Batch.FIELD_NAME, null);
            selector.put(FieldName.Batch.FIELD_INNER_CREATE_TIME, null);
            selector.put(FieldName.Batch.FIELD_FILES, null);
            cursor = ScmContentModule.getInstance().getMetaService().getBatchList(wsInfo.getName(),
                    matcher, selector, orderBy, skip, limit);
            return new BatchMetaCursorFillInFileCount(cursor);
        }
        catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
            logger.error("query failed: workspace={}, matcher={}", wsInfo.getName(), matcher);
            throw e;
        }
    }

    @Override
    public void attachFile(ScmWorkspaceInfo wsInfo, String batchId, String batchCreateMonth,
            String fileId, String user) throws ScmServerException {
        try {
            ScmContentModule.getInstance().getMetaService().batchAttachFile(wsInfo, batchId,
                    batchCreateMonth, fileId, user);
        }
        catch (ScmServerException e) {
            logger.error("attachFile failed: workspace={}, batchId={}, fileId={}", wsInfo.getName(),
                    batchId, fileId);
            throw e;
        }
    }

    @Override
    public void detachFile(ScmWorkspaceInfo wsInfo, String batchId, String batchCreateMonth,
            String fileId, String updateUser) throws ScmServerException {
        try {
            ScmContentModule.getInstance().getMetaService().batchDetachFile(wsInfo.getName(),
                    batchId, batchCreateMonth, fileId, updateUser);
        }
        catch (ScmServerException e) {
            logger.error("detachFile failed: workspace={}, batchId={},fileId={}", wsInfo.getName(),
                    batchId, fileId);
            throw e;
        }
    }

    @Override
    public boolean updateById(ScmWorkspaceInfo wsInfo, String batchId, String batchCreateMonth,
            BSONObject updator, String user) throws ScmServerException {
        try {
            checkUpdateObj(updator);

            // check class properties
            String classIdKey = FieldName.Batch.FIELD_CLASS_ID;
            String propertiesKey = FieldName.Batch.FIELD_CLASS_PROPERTIES;
            MetaDataManager metaDataManager = MetaDataManager.getInstence();
            if (updator.containsField(classIdKey) || updator.containsField(propertiesKey)
                    || metaDataManager.isUpdateSingleClassProperty(updator, propertiesKey)) {
                ScmMetaService metaService = ScmContentModule.getInstance().getMetaService();
                BSONObject oldbatch = metaService.getBatchInfo(wsInfo, batchId, batchCreateMonth);
                if (oldbatch == null) {
                    return false;
                }
                String classId = (String) oldbatch.get(classIdKey);
                metaDataManager.checkUpdateProperties(wsInfo.getName(), updator, classIdKey,
                        propertiesKey, classId);
            }

            // add modify user and time
            updator.put(FieldName.Batch.FIELD_INNER_UPDATE_USER, user);
            Date updateTime = new Date();
            updator.put(FieldName.Batch.FIELD_INNER_UPDATE_TIME, updateTime.getTime());

            logger.info("updating batch:wsName=" + wsInfo.getName() + ",batchId=" + batchId
                    + ",user=" + user + ",updator=" + updator.toString());

            boolean ret = ScmContentModule.getInstance().getMetaService()
                    .updateBatchInfo(wsInfo.getName(), batchId, batchCreateMonth, updator);
            return ret;
        }
        catch (ScmServerException e) {
            logger.error("updateById failed:batchId={},updator={}", batchId, updator);
            throw e;
        }
    }

    private String addExtraFields(ScmWorkspaceInfo ws, BSONObject obj, String userName)
            throws ScmServerException {
        obj.put(FieldName.Batch.FIELD_INNER_CREATE_USER, userName);
        obj.put(FieldName.Batch.FIELD_INNER_UPDATE_USER, userName);
        obj.put(FieldName.Batch.FIELD_FILES, new BasicBSONList());

        String clientBatchId = (String) obj.get(FieldName.Batch.FIELD_ID);

        // 不分区
        if (!ws.isBatchSharding()) {
            Date createDate = new Date();
            if (clientBatchId == null) {
                // 不指定ID
                String batchId = ScmIdGenerator.BatchId.get(createDate);
                obj.put(FieldName.Batch.FIELD_ID, batchId);
                obj.put(FieldName.Batch.FIELD_INNER_UPDATE_TIME, createDate.getTime());
                obj.put(FieldName.Batch.FIELD_INNER_CREATE_TIME, createDate.getTime());
                obj.put(FieldName.Batch.FIELD_INNER_CREATE_MONTH,
                        ScmSystemUtils.getCurrentYearMonth(createDate));
                return batchId;
            }
            // 指定ID
            obj.put(FieldName.Batch.FIELD_INNER_UPDATE_TIME, createDate.getTime());
            obj.put(FieldName.Batch.FIELD_INNER_CREATE_TIME, createDate.getTime());
            obj.put(FieldName.Batch.FIELD_INNER_CREATE_MONTH,
                    ScmSystemUtils.getCurrentYearMonth(createDate));
            return clientBatchId;
        }

        // 分区
        if (clientBatchId == null) {
            // 不指定ID
            if (!ws.isBatchUseSystemId()) {
                throw new ScmInvalidArgumentException("please specify batch id:ws=" + ws.getName());
            }
            Date createDate = new Date();
            String batchId = ScmIdGenerator.BatchId.get(createDate);
            obj.put(FieldName.Batch.FIELD_ID, batchId);
            obj.put(FieldName.Batch.FIELD_INNER_UPDATE_TIME, createDate.getTime());
            obj.put(FieldName.Batch.FIELD_INNER_CREATE_TIME, createDate.getTime());
            obj.put(FieldName.Batch.FIELD_INNER_CREATE_MONTH,
                    ScmSystemUtils.getCurrentYearMonth(createDate));
            return batchId;
        }
        // 指定 ID
        if (ws.isBatchUseSystemId()) {
            throw new ScmInvalidArgumentException("can not specify batch id:ws=" + ws.getName());
        }
        Date clientIdDate = ScmSystemUtils.getDateFromCustomBatchId(clientBatchId,
                ws.getBatchIdTimeRegex(), ws.getBatchIdTimePattern());

        obj.put(FieldName.Batch.FIELD_INNER_UPDATE_TIME, clientIdDate.getTime());
        obj.put(FieldName.Batch.FIELD_INNER_CREATE_TIME, clientIdDate.getTime());
        obj.put(FieldName.Batch.FIELD_INNER_CREATE_MONTH,
                ScmSystemUtils.getCurrentYearMonth(clientIdDate));
        return clientBatchId;

    }

    private BSONObject checkCreateObj(BSONObject batchObj) throws ScmServerException {
        BSONObject result = new BasicBSONObject();

        result.put(FieldName.Batch.FIELD_ID, batchObj.get(FieldName.Batch.FIELD_ID));

        String fieldName = FieldName.Batch.FIELD_NAME;
        result.put(fieldName, checkExistString(batchObj, fieldName));

        fieldName = FieldName.Batch.FIELD_CLASS_ID;
        result.put(fieldName, batchObj.get(fieldName));

        fieldName = FieldName.Batch.FIELD_CLASS_PROPERTIES;
        BSONObject classValue = (BSONObject) batchObj.get(fieldName);
        result.put(fieldName, ScmArgumentChecker.checkAndCorrectClass(classValue, fieldName));

        fieldName = FieldName.Batch.FIELD_TAGS;
        BSONObject tagsValue = (BSONObject) batchObj.get(fieldName);
        result.put(fieldName, ScmArgumentChecker.checkAndCorrectTags(tagsValue, fieldName));

        return result;
    }

    private void checkUpdateObj(BSONObject batchObj) throws ScmServerException {
        Set<String> objFields = batchObj.keySet();

        // support classId & properties at a time.
        // other properties support only one at a time.
        if (objFields.size() != 1) {
            if (objFields.size() != 2) {
                throw new ScmInvalidArgumentException(
                        "invlid argument,updates only one property at a time:updator=" + batchObj);
            }

            // key number = 2
            if (!objFields.contains(FieldName.FIELD_CLFILE_PROPERTIES)
                    || !objFields.contains(FieldName.FIELD_CLFILE_FILE_CLASS_ID)) {
                // must contain id and properties
                throw new ScmInvalidArgumentException(
                        "invlid argument,updates only classId and properties at a time:updator="
                                + batchObj);
            }
        }

        Set<String> availableFields = new HashSet<>();
        availableFields.add(FieldName.Batch.FIELD_NAME);
        availableFields.add(FieldName.Batch.FIELD_CLASS_ID);
        availableFields.add(FieldName.Batch.FIELD_CLASS_PROPERTIES);
        availableFields.add(FieldName.Batch.FIELD_TAGS);
        for (String field : objFields) {
            // SEQUOIACM-312
            if (field.startsWith(FieldName.Batch.FIELD_CLASS_PROPERTIES + ".")) {
                String subKey = field
                        .substring((FieldName.Batch.FIELD_CLASS_PROPERTIES + ".").length());
                MetaDataManager.getInstence().validateKeyFormat(subKey,
                        FieldName.Batch.FIELD_CLASS_PROPERTIES);
            }
            else if (!availableFields.contains(field)) {
                throw new ScmOperationUnsupportedException(
                        "field can't be modified:fieldName=" + field);
            }
        }

        // value type is string. and can't be null
        Set<String> valueCheckStringFields = new HashSet<>();
        valueCheckStringFields.add(FieldName.Batch.FIELD_NAME);
        valueCheckStringFields.add(FieldName.Batch.FIELD_CLASS_ID);
        for (String field : valueCheckStringFields) {
            if (batchObj.containsField(field)) {
                ScmMetaSourceHelper.checkExistString(batchObj, field);
            }
        }

        // value type is bson, check the format
        String fieldName = FieldName.Batch.FIELD_CLASS_PROPERTIES;
        if (batchObj.containsField(fieldName)) {
            BSONObject classValue = (BSONObject) batchObj.get(fieldName);
            batchObj.put(fieldName, ScmArgumentChecker.checkAndCorrectClass(classValue, fieldName));
        }
        fieldName = FieldName.Batch.FIELD_TAGS;
        if (batchObj.containsField(fieldName)) {
            BSONObject tagsValue = (BSONObject) batchObj.get(fieldName);
            batchObj.put(fieldName, ScmArgumentChecker.checkAndCorrectTags(tagsValue, fieldName));
        }
    }

    private Object checkExistString(BSONObject obj, String fieldName) throws ScmServerException {
        Object value = obj.get(fieldName);
        if (value == null) {
            throw new ScmInvalidArgumentException("field [" + fieldName + "] is not exist!");
        }

        if (!(value instanceof String)) {
            throw new ScmInvalidArgumentException(
                    "field [" + fieldName + "] is not String format!");
        }

        return value;
    }

}
