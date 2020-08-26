package com.sequoiacm.contentserver.listener;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;

@Component
public class FileOperationListenerMgr implements FileOperationListener {
    private List<FileOperationListener> listeners;
    private static final Logger logger = LoggerFactory.getLogger(FileOperationListenerMgr.class);

    @Autowired
    public FileOperationListenerMgr(List<FileOperationListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public OperationCompleteCallback postCreate(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {
        try {
            CallbackList ret = new CallbackList();
            for (FileOperationListener l : listeners) {
                ret.add(l.postCreate(ws, fileId));
            }
            return ret;
        }
        catch (Exception e) {
            logger.warn("failed to do post create:ws=" + ws.getName() + ", file=" + fileId, e);
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
    }

    @Override
    public void preCreate(ScmWorkspaceInfo ws, BSONObject file) throws ScmServerException {
        for (FileOperationListener l : listeners) {
            l.preCreate(ws, file);
        }
    }

    @Override
    public OperationCompleteCallback postUpdate(ScmWorkspaceInfo ws, BSONObject fileInfo)
            throws ScmServerException {
        try {
            CallbackList ret = new CallbackList();
            for (FileOperationListener l : listeners) {
                ret.add(l.postUpdate(ws, fileInfo));
            }
            return ret;
        }
        catch (Exception e) {
            String fileId = BsonUtils.getStringChecked(fileInfo, FieldName.FIELD_CLFILE_ID);
            logger.warn("failed to do post update:ws=" + ws.getName() + ", file=" + fileId, e);
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
    }

    @Override
    public OperationCompleteCallback postUpdateContent(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {
        try {
            CallbackList ret = new CallbackList();
            for (FileOperationListener l : listeners) {
                ret.add(l.postUpdateContent(ws, fileId));
            }
            return ret;
        }
        catch (Exception e) {
            logger.warn("failed to do post udpate content:ws=" + ws.getName() + ", file=" + fileId,
                    e);
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
    }

    @Override
    public void postDelete(ScmWorkspaceInfo ws, List<BSONObject> allFileVersions)
            throws ScmServerException {
        try {
            for (FileOperationListener l : listeners) {
                l.postDelete(ws, allFileVersions);
            }
        }
        catch (Exception e) {
            String fileId = BsonUtils.getStringChecked(allFileVersions.get(0),
                    FieldName.FIELD_CLFILE_ID);
            logger.warn("failed to do post delete:ws=" + ws.getName() + ", file=" + fileId, e);
        }
    }

    @Override
    public void preUpdateContent(ScmWorkspaceInfo ws, BSONObject newVersionFile)
            throws ScmServerException {
        for (FileOperationListener l : listeners) {
            l.preUpdateContent(ws, newVersionFile);
        }
    }
}

class CallbackList implements OperationCompleteCallback {
    private List<OperationCompleteCallback> callbacks = new ArrayList<>();

    public void add(OperationCompleteCallback c) {
        callbacks.add(c);
    }

    @Override
    public void onComplete() {
        for (OperationCompleteCallback c : callbacks) {
            c.onComplete();
        }
    }

}
