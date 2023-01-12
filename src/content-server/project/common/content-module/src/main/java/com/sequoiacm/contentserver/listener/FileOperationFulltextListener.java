package com.sequoiacm.contentserver.listener;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.config.ServerConfig;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOpFeedback;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperation;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperation.OperationType;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.mq.client.EnableScmMqAdmin;
import com.sequoiacm.mq.client.EnableScmMqProducer;
import com.sequoiacm.mq.client.core.FeedbackCallback;
import com.sequoiacm.mq.client.core.ProducerClient;
import com.sequoiacm.mq.client.core.SerializableMessage;
import com.sequoiacm.mq.core.exception.MqException;

@Component
@EnableScmMqAdmin
@EnableScmMqProducer
public class FileOperationFulltextListener implements FileOperationListener {
    private static final Logger logger = LoggerFactory
            .getLogger(FileOperationFulltextListener.class);

    @Autowired
    private ProducerClient producerClient;

    @Autowired
    private ServerConfig serverConfig;

    @Override
    public OperationCompleteCallback postAddVersion(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {
        return postCreate(ws, fileId);
    }

    @Override
    public void postDeleteVersion(ScmWorkspaceInfo ws, FileMeta deletedVersion)
            throws ScmServerException {
        ScmWorkspaceFulltextExtData fulltextExt = ws.getFulltextExtData();

        if (!fulltextExt.isEnabled()) {
            return;
        }

        ScmFileFulltextExtData fileExtData = new ScmFileFulltextExtData(
                deletedVersion.getExternalData());
        if (fileExtData.getIdxStatus() == ScmFileFulltextStatus.CREATED) {
            putDropIndexMsg(fulltextExt, deletedVersion.getId(), fileExtData.getIdxDocumentId());
        }
    }

    @Override
    public OperationCompleteCallback postUpdate(ScmWorkspaceInfo ws,
            FileMeta latestVersionAfterUpdate)
            throws ScmServerException {
        ScmWorkspaceFulltextExtData wsFulltextExt = ws.getFulltextExtData();
        if (!wsFulltextExt.isEnabled()) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }

        ScmFileFulltextExtData fileExtData = new ScmFileFulltextExtData(
                latestVersionAfterUpdate.getExternalData());
        String fileId = latestVersionAfterUpdate.getId();
        boolean isMatchFulltext = getFileIfMatchFulltextCondition(ws, fileId,
                wsFulltextExt.getFileMatcher()) != null;
        if (isMatchFulltext) {
            if (fileExtData.getIdxStatus() == ScmFileFulltextStatus.CREATED) {
                return OperationCompleteCallback.EMPTY_CALLBACK;
            }
            if (wsFulltextExt.getMode() == ScmFulltextMode.async) {
                putCreateIndexMsg(wsFulltextExt, fileId, null);
                return OperationCompleteCallback.EMPTY_CALLBACK;
            }

            FulltextIndexCreateFeedbackCallback feedbackCallback = new FulltextIndexCreateFeedbackCallback(
                    ws.getName(), fileId);
            long msgId = putCreateIndexMsg(wsFulltextExt, fileId, feedbackCallback);
            return new WaitMsgConsumedCallback(feedbackCallback, ws.getName(), fileId, msgId,
                    serverConfig.getFulltextCreateTimeout());
        }

        if (fileExtData.getIdxStatus() == ScmFileFulltextStatus.NONE) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
        putDropIndexMsg(wsFulltextExt, fileId);
        return OperationCompleteCallback.EMPTY_CALLBACK;
    }

    @Override
    public void postDelete(ScmWorkspaceInfo ws, List<FileMeta> allFileVersions)
            throws ScmServerException {
        ScmWorkspaceFulltextExtData fulltextExt = ws.getFulltextExtData();

        if (!fulltextExt.isEnabled()) {
            return;
        }

        for (FileMeta f : allFileVersions) {
            ScmFileFulltextExtData fileExtData = new ScmFileFulltextExtData(
                    f.getExternalData());
            if (fileExtData.getIdxStatus() == ScmFileFulltextStatus.CREATED) {
                putDropIndexMsgForDeleteFileSilence(fulltextExt, f.getId());
                return;
            }
        }
    }

    @Override
    public OperationCompleteCallback postCreate(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {

        ScmWorkspaceFulltextExtData fulltextExt = ws.getFulltextExtData();

        if (!fulltextExt.isEnabled()) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }

        BSONObject file = getFileIfMatchFulltextCondition(ws, fileId, fulltextExt.getFileMatcher());
        if (file == null) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }

        if (fulltextExt.getMode() == ScmFulltextMode.async) {
            putCreateIndexMsg(fulltextExt, fileId, null);
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }

        FulltextIndexCreateFeedbackCallback feedbackCallback = new FulltextIndexCreateFeedbackCallback(
                ws.getName(), fileId);
        long msgId = putCreateIndexMsg(fulltextExt, fileId, feedbackCallback);

        if (fulltextExt.getMode() == ScmFulltextMode.async) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
        return new WaitMsgConsumedCallback(feedbackCallback, ws.getName(), fileId, msgId,
                serverConfig.getFulltextCreateTimeout());
    }

    private long putCreateIndexMsg(ScmWorkspaceFulltextExtData fulltextExt, String fileId,
            FulltextIndexCreateFeedbackCallback feedbackCallback) throws ScmServerException {
        FileFulltextOperation msg = new FileFulltextOperation();
        msg.setFileId(fileId);
        msg.setIndexLocation(fulltextExt.getIndexDataLocation());
        msg.setOperationType(OperationType.CREATE_IDX);
        msg.setWsName(fulltextExt.getWsName());
        msg.setSyncSaveIndex(true);
        msg.setReindex(false);
        try {
            long msgId = producerClient.putMsg(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                    fulltextExt.getWsName() + "-" + fileId, new FulltextMsgWrapper(msg),
                    serverConfig.getFulltextCreateTimeout(), feedbackCallback);
            logger.debug("put fulltext msg to mq-server:" + msg);
            return msgId;
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to put fulltext msg to  mq-server:topic="
                            + FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC + ", msg=" + msg,
                    e);
        }
    }

    private void putDropIndexMsgForDeleteFileSilence(ScmWorkspaceFulltextExtData fulltextExt,
            String fileId) {
        FileFulltextOperation msg = new FileFulltextOperation();
        msg.setFileId(fileId);
        msg.setIndexLocation(fulltextExt.getIndexDataLocation());
        msg.setOperationType(OperationType.DROP_IDX_ONLY);
        msg.setWsName(fulltextExt.getWsName());
        try {
            producerClient.putMsg(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                    fulltextExt.getWsName() + "-" + fileId, new FulltextMsgWrapper(msg));
            logger.debug("put fulltext msg to mq-server:" + msg);
        }
        catch (Exception e) {
            logger.warn("failed to put fulltext msg to  mq-server:topic={}, msg={}",
                    FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC, msg, e);
        }
    }

    private void putDropIndexMsg(ScmWorkspaceFulltextExtData fulltextExt, String fileId,
            String indexDocId) throws ScmServerException {
        FileFulltextOperation msg = new FileFulltextOperation();
        msg.setFileId(fileId);
        msg.setIndexDocId(indexDocId);
        msg.setIndexLocation(fulltextExt.getIndexDataLocation());
        msg.setOperationType(OperationType.DROP_SPECIFY_IDX_ONLY);
        msg.setWsName(fulltextExt.getWsName());

        try {
            producerClient.putMsg(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                    fulltextExt.getWsName() + "-" + fileId, new FulltextMsgWrapper(msg));
            logger.debug("put fulltext msg to mq-server:" + msg);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to put fulltext msg to  mq-server:topic="
                            + FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC + ", msg=" + msg,
                    e);
        }
    }

    private void putDropIndexMsg(ScmWorkspaceFulltextExtData fulltextExt, String fileId)
            throws ScmServerException {
        FileFulltextOperation msg = new FileFulltextOperation();
        msg.setFileId(fileId);
        msg.setIndexLocation(fulltextExt.getIndexDataLocation());
        msg.setOperationType(OperationType.DROP_IDX_AND_UPDATE_FILE);
        msg.setWsName(fulltextExt.getWsName());
        try {
            producerClient.putMsg(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                    fulltextExt.getWsName() + "-" + fileId, new FulltextMsgWrapper(msg));
            logger.debug("put fulltext msg to mq-server:" + msg);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to put fulltext msg to  mq-server:topic="
                            + FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC + ", msg=" + msg,
                    e);
        }
    }

    // return null if not match, else return the file info
    private BSONObject getFileIfMatchFulltextCondition(ScmWorkspaceInfo wsInfo, String fileId,
            BSONObject fulltextFileMatcher) throws ScmServerException {

        BasicBSONObject fileCondition = new BasicBSONObject();
        fileCondition.put(FieldName.FIELD_CLFILE_ID, fileId);
        ScmMetaSourceHelper.addFileIdAndCreateMonth(fileCondition, fileId);

        BasicBSONList andArr = new BasicBSONList();
        andArr.add(fileCondition);
        andArr.add(fulltextFileMatcher);
        BasicBSONObject condition = new BasicBSONObject("$and", andArr);
        MetaCursor cursor = null;
        try {
            cursor = ScmContentModule.getInstance().getMetaService()
                    .queryCurrentFile(wsInfo, condition, null, null, 0, 1, false);
            return cursor.getNext();
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to put fulltext msg to  mq-server:ws=" + wsInfo.getName() + ", file="
                            + fileId,
                    e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void preCreate(ScmWorkspaceInfo ws, FileMeta file) throws ScmServerException {
        BSONObject extData = file.getExternalData();
        if (extData == null) {
            extData = new BasicBSONObject();
            file.setExternalData(extData);
        }
        extData.putAll(new ScmFileFulltextExtData().toBson());
    }

    @Override
    public void preAddVersion(ScmWorkspaceInfo ws, FileMeta newVersionFile)
            throws ScmServerException {
        preCreate(ws, newVersionFile);
    }

}

class WaitMsgConsumedCallback implements OperationCompleteCallback {
    private static final Logger logger = LoggerFactory.getLogger(WaitMsgConsumedCallback.class);
    private int timeout;
    private long msgId;
    private String fileId;
    private String ws;
    private FulltextIndexCreateFeedbackCallback feedbackCallback;

    public WaitMsgConsumedCallback(FulltextIndexCreateFeedbackCallback feedbackCallback, String ws,
            String fileID, long msgId, int timeout) {
        this.feedbackCallback = feedbackCallback;
        this.ws = ws;
        this.fileId = fileID;
        this.msgId = msgId;
        this.timeout = timeout;
    }

    @Override
    public void onComplete() {
        try {
            feedbackCallback.waitFeedback(timeout);
        }
        catch (Exception e) {
            logger.warn("failed to wait for fulltext index to create:ws={}, fileId={}, timeout={}",
                    ws, fileId, timeout, e);
        }
    }
}

class FulltextMsgWrapper implements SerializableMessage {
    private FileFulltextOperation message;

    public FulltextMsgWrapper(FileFulltextOperation m) {
        this.message = m;
    }

    @Override
    public BSONObject serialize() {
        return message.toBSON();
    }
}

class FulltextIndexCreateFeedbackCallback extends FeedbackCallback<FileFulltextOpFeedback> {
    private static final Logger logger = LoggerFactory
            .getLogger(FulltextIndexCreateFeedbackCallback.class);

    private final CountDownLatch countdownLatch;
    private final String fileId;
    private final String wsName;

    public FulltextIndexCreateFeedbackCallback(String wsName, String fileId) {
        super(FulltextCommonDefine.FULLTEXT_GROUP_NAME);
        this.fileId = fileId;
        this.wsName = wsName;
        countdownLatch = new CountDownLatch(1);
    }

    @Override
    public void onFeedback(String topic, String key, long msgId,
            FileFulltextOpFeedback feedbackContent) {
        logger.debug("file create index feedback:ws={}, fileId={}, topic={}, msgId={}, feedback={}",
                wsName, fileId, topic, msgId, feedbackContent);
        countdownLatch.countDown();
    }

    @Override
    public void onTimeout(String topic, String key, long msgId) {
        logger.warn(
                "failed to wait file create index, cause by msg feedback timeout:ws={}, fileId={}, topic={}, msgId={}",
                wsName, fileId, topic, msgId);
        countdownLatch.countDown();
    }

    @Override
    protected FileFulltextOpFeedback convert(BSONObject feedback) throws MqException {
        return new FileFulltextOpFeedback(feedback);
    }

    public void waitFeedback(long timeout) throws InterruptedException {
        boolean awaitSuccess = countdownLatch.await(timeout, TimeUnit.MILLISECONDS);
        if (!awaitSuccess) {
            logger.warn(
                    "failed to wait file create index, cause by msg feedback timeout, remove feedback callback:ws={}, fileId={}",
                    wsName, fileId);
            unregisterCallback();
        }
    }
}
