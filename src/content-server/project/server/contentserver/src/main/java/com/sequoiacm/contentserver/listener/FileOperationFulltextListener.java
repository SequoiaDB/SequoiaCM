package com.sequoiacm.contentserver.listener;

import java.util.List;

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
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg.OptionType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.mq.client.EnableScmMqAdmin;
import com.sequoiacm.mq.client.EnableScmMqProducer;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ProducerClient;
import com.sequoiacm.mq.client.core.SerializeableMessage;

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
    @Autowired
    private AdminClient mqAdmin;

    @Override
    public OperationCompleteCallback postUpdateContent(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {
        return postCreate(ws, fileId);
    }

    @Override
    public OperationCompleteCallback postUpdate(ScmWorkspaceInfo ws, BSONObject oldFile)
            throws ScmServerException {
        ScmWorkspaceFulltextExtData wsFulltextExt = ws.getFulltextExtData();
        if (!wsFulltextExt.isEnabled()) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }

        ScmFileFulltextExtData fileExtData = new ScmFileFulltextExtData(
                BsonUtils.getBSON(oldFile, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA));

        String fileId = BsonUtils.getStringChecked(oldFile, FieldName.FIELD_CLFILE_ID);

        boolean isMatchFulltext = getFileIfMatchFulltextCondition(ws, fileId,
                wsFulltextExt.getFileMatcher()) != null;
        if (isMatchFulltext) {
            if (fileExtData.getIdxStatus() == ScmFileFulltextStatus.CREATED) {
                return OperationCompleteCallback.EMPTY_CALLBACK;
            }
            long msgId = putCreateIndexMsg(wsFulltextExt, fileId);

            if (wsFulltextExt.getMode() == ScmFulltextMode.async) {
                return OperationCompleteCallback.EMPTY_CALLBACK;
            }

            return new WaitMsgConsumedCallback(mqAdmin, ws.getName(), fileId, msgId,
                    serverConfig.getFulltextCreateTimeout());
        }

        if (fileExtData.getIdxStatus() == ScmFileFulltextStatus.NONE) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
        putDropIndexMsg(wsFulltextExt, fileId);
        return OperationCompleteCallback.EMPTY_CALLBACK;
    }

    @Override
    public void postDelete(ScmWorkspaceInfo ws, List<BSONObject> allFileVersions)
            throws ScmServerException {
        ScmWorkspaceFulltextExtData fulltextExt = ws.getFulltextExtData();

        if (!fulltextExt.isEnabled()) {
            return;
        }

        for (BSONObject f : allFileVersions) {
            ScmFileFulltextExtData fileExtData = new ScmFileFulltextExtData(
                    BsonUtils.getBSON(f, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA));
            if (fileExtData.getIdxStatus() == ScmFileFulltextStatus.CREATED) {
                String fileId = BsonUtils.getStringChecked(f, FieldName.FIELD_CLFILE_ID);
                putDropIndexMsgForDeleteFile(fulltextExt, fileId);
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

        long msgId = putCreateIndexMsg(fulltextExt, fileId);

        if (fulltextExt.getMode() == ScmFulltextMode.async) {
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
        return new WaitMsgConsumedCallback(mqAdmin, ws.getName(), fileId, msgId,
                serverConfig.getFulltextCreateTimeout());
    }

    private long putCreateIndexMsg(ScmWorkspaceFulltextExtData fulltextExt, String fileId)
            throws ScmServerException {
        FulltextMsg msg = new FulltextMsg();
        msg.setFileId(fileId);
        msg.setIndexLocation(fulltextExt.getIndexDataLocation());
        msg.setOptionType(OptionType.CREATE_IDX);
        msg.setWsName(fulltextExt.getWsName());
        String topic = fulltextExt.getWsName() + FulltextCommonDefine.FULLTEXT_TOPIC_TAIL;
        try {
            long msgId = producerClient.putMsg(topic, fileId, new FulltextMsgWrapper(msg));
            logger.debug("put fulltext msg to mq-server:" + msg);
            return msgId;
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to put fulltext msg to  mq-server:topic=" + topic + ", msg=" + msg, e);
        }
    }

    private void putDropIndexMsgForDeleteFile(ScmWorkspaceFulltextExtData fulltextExt,
            String fileId) throws ScmServerException {
        FulltextMsg msg = new FulltextMsg();
        msg.setFileId(fileId);
        msg.setIndexLocation(fulltextExt.getIndexDataLocation());
        msg.setOptionType(OptionType.DROP_IDX_ONLY);
        msg.setWsName(fulltextExt.getWsName());
        String topic = fulltextExt.getWsName() + FulltextCommonDefine.FULLTEXT_TOPIC_TAIL;
        try {
            producerClient.putMsg(topic, fileId, new FulltextMsgWrapper(msg));
            logger.debug("put fulltext msg to mq-server:" + msg);
        }
        catch (Exception e) {
            logger.warn("failed to put fulltext msg to  mq-server:topic={}, msg={}", topic, msg, e);
        }
    }

    private void putDropIndexMsg(ScmWorkspaceFulltextExtData fulltextExt, String fileId)
            throws ScmServerException {
        FulltextMsg msg = new FulltextMsg();
        msg.setFileId(fileId);
        msg.setIndexLocation(fulltextExt.getIndexDataLocation());
        msg.setOptionType(OptionType.DROP_IDX_AND_UPDATE_FILE);
        msg.setWsName(fulltextExt.getWsName());
        String topic = fulltextExt.getWsName() + FulltextCommonDefine.FULLTEXT_TOPIC_TAIL;
        try {
            producerClient.putMsg(topic, fileId, new FulltextMsgWrapper(msg));
            logger.debug("put fulltext msg to mq-server:" + msg);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "failed to put fulltext msg to  mq-server:topic=" + topic + ", msg=" + msg, e);
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
            cursor = ScmContentServer.getInstance().getMetaService().queryCurrentFile(
                    wsInfo.getMetaLocation(), wsInfo.getName(), condition, null, null, 0, 1);
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
    public void preCreate(ScmWorkspaceInfo ws, BSONObject file) throws ScmServerException {
        BSONObject extData = BsonUtils.getBSON(file, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA);
        if (extData == null) {
            extData = new BasicBSONObject();
            file.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA, extData);
        }
        extData.putAll(new ScmFileFulltextExtData().toBson());
    }

    @Override
    public void preUpdateContent(ScmWorkspaceInfo ws, BSONObject newVersionFile)
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
    private AdminClient mqAdmin;

    public WaitMsgConsumedCallback(AdminClient mqAdmin, String ws, String fileID, long msgId,
            int timeout) {
        this.mqAdmin = mqAdmin;
        this.ws = ws;
        this.fileId = fileID;
        this.msgId = msgId;
        this.timeout = timeout;
    }

    @Override
    public void onComplete() {
        try {
            boolean isConsumed = mqAdmin.waitForMsgConusmed(
                    ws + FulltextCommonDefine.FULLTEXT_TOPIC_TAIL, msgId, timeout, 300);
            if (!isConsumed) {
                logger.warn(
                        "failed to wait for fulltext index to create, cause by timeout:ws={}, fileId={}, timeout={}",
                        ws, fileId, timeout);
            }
        }
        catch (Exception e) {
            logger.warn("failed to wait for fulltext index to create:ws={}, fileId={}, timeout={}",
                    ws, fileId);
        }
    }

}

class FulltextMsgWrapper implements SerializeableMessage {
    private FulltextMsg message;

    public FulltextMsgWrapper(FulltextMsg m) {
        this.message = m;
    }

    @Override
    public BSONObject serialize() {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FulltextMsg.KEY_FILE_ID, message.getFileId());
        ret.put(FulltextMsg.KEY_IDX_LOCATION, message.getIndexLocation());
        ret.put(FulltextMsg.KEY_OPTION_TYPE, message.getOptionType().name());
        ret.put(FulltextMsg.KEY_WS_NAME, message.getWsName());
        return ret;
    }
}
