package com.sequoiacm.s3.cursor;

import java.util.List;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.GreatThanOrEquals;
import com.sequoiacm.s3.remote.ScmContentServerClient;
import com.sequoiacm.s3.remote.ScmDirInfo;
import com.sequoiacm.s3.remote.ScmFileInfo;

public class ScmFileDirCursor extends FileDirCursor<ScmFileInfo, ScmDirInfo> {
    private ScmContentServerClient client;

    public ScmFileDirCursor(ScmContentServerClient client, String parentDir, String startWith,
            String greaterThan) throws S3ServerException {
        super(parentDir, startWith, greaterThan);
        this.client = client;
    }

    @Override
    protected ScmDirInfo getDir(String path) throws S3ServerException {
        try {
            return client.getDir(path);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                return null;
            }
            throw new S3ServerException(S3Error.SCM_GET_DIR_FAILED,
                    "failed to get dir:ws=" + client.getWs() + ", path=" + path, e);
        }
    }

    @Override
    protected List<ScmDirInfo> getDirs(ScmDirInfo parentDir, String startWith,
            GreatThanOrEquals gtOrEq, int fetchNum) throws S3ServerException {
        try {
            return client.getDirs(parentDir.getId(), startWith, gtOrEq, fetchNum);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() != ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw new S3ServerException(S3Error.SCM_GET_DIR_FAILED,
                        "failed to list dir:ws=" + client.getWs() + ",parentDirId="
                                + parentDir.getId() + ",startWith=" + startWith
                                + ",greaterThanOrEq=" + gtOrEq,
                        e);
            }
            return null;
        }
    }

    @Override
    protected List<ScmFileInfo> getFiles(ScmDirInfo parentDir, String startWith, String greaterThan,
            int fetchNum) throws S3ServerException {
        try {
            return client.getFiles(parentDir.getId(), startWith, greaterThan, fetchNum);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                return null;
            }
            throw new S3ServerException(S3Error.SCM_GET_FILE_FAILED,
                    "failed to list file:ws=" + client.getWs() + ",parentDirId=" + parentDir.getId()
                            + ",startWith=" + startWith + ",greaterThan=" + greaterThan,
                    e);
        }
    }

    @Override
    protected String getDirName(ScmDirInfo dir) {
        return dir.getName();
    }

    @Override
    protected String getFileName(ScmFileInfo file) {
        return file.getName();
    }

    @Override
    protected FileDirInfo createFileInstanceByDir(ScmDirInfo parent, ScmDirInfo dir) {
        return new FileDirInfo(getParentDir(), dir);
    }

    @Override
    protected FileDirInfo createFileInstanceByFile(ScmDirInfo parent, ScmFileInfo file) {
        return new FileDirInfo(getParentDir(), file);
    }

}
