package com.sequoiacm.contentserver.listener;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.exception.ScmServerException;

@Component
public class FileOperationListenerMgr implements FileOperationListener {
    private List<FileOperationListener> listeners;
    private static final Logger logger = LoggerFactory.getLogger(FileOperationListenerMgr.class);

    @Autowired
    public FileOperationListenerMgr(List<FileOperationListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    @SlowLog(operation = "postCreate")
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
    @SlowLog(operation = "preCreate")
    public void preCreate(ScmWorkspaceInfo ws, FileMeta file) throws ScmServerException {
        for (FileOperationListener l : listeners) {
            l.preCreate(ws, file);
        }
    }

    @Override
    public OperationCompleteCallback postUpdate(ScmWorkspaceInfo ws,
            FileMeta latestVersionBeforeUpdate, FileMeta latestVersionAfterUpdate)
            throws ScmServerException {
        try {
            CallbackList ret = new CallbackList();
            for (FileOperationListener l : listeners) {
                ret.add(l.postUpdate(ws, latestVersionBeforeUpdate, latestVersionAfterUpdate));
            }
            return ret;
        }
        catch (Exception e) {
            logger.warn("failed to do post update:ws=" + ws.getName() + ", file="
                    + latestVersionAfterUpdate.getId(), e);
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
    }

    @Override
    public OperationCompleteCallback postAddVersion(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException {
        try {
            CallbackList ret = new CallbackList();
            for (FileOperationListener l : listeners) {
                ret.add(l.postAddVersion(ws, fileId));
            }
            return ret;
        }
        catch (Exception e) {
            logger.warn("failed to do post add version: ws=" + ws.getName() + ", file=" + fileId,
                    e);
            return OperationCompleteCallback.EMPTY_CALLBACK;
        }
    }

    @Override
    public void postDeleteVersion(ScmWorkspaceInfo ws, FileMeta deletedVersion) {
        try {
            for (FileOperationListener l : listeners) {
                l.postDeleteVersion(ws, deletedVersion);
            }
        }
        catch (Exception e) {
            logger.warn("failed to do post delete version: ws=" + ws.getName() + ", deletedVersion="
                    + deletedVersion, e);
        }
    }

    @Override
    public void postDelete(ScmWorkspaceInfo ws, List<FileMeta> allFileVersions)
            throws ScmServerException {
        if (allFileVersions == null || allFileVersions.size() <= 0) {
            return;
        }
        try {
            for (FileOperationListener l : listeners) {
                l.postDelete(ws, allFileVersions);
            }
        }
        catch (Exception e) {
            logger.warn("failed to do post delete:ws=" + ws.getName() + ", file="
                    + allFileVersions.get(0).getId(), e);
        }
    }

    @Override
    public void preAddVersion(ScmWorkspaceInfo ws, FileMeta newVersionFile)
            throws ScmServerException {
        for (FileOperationListener l : listeners) {
            l.preAddVersion(ws, newVersionFile);
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
