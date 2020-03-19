package com.sequoiacm.s3.cursor;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.GreatThanOrEquals;

public abstract class DirCursor<D> {
    private static final int FETCH_NUM = 50;
    private String parentDir;
    private D parentDirInfo;
    private boolean nomoreDir;
    private LinkedList<D> dirs = new LinkedList<>();
    private String dirGreaterThan;

    // return null if path not exist!
    protected abstract D getDir(String path) throws S3ServerException;

    // return null if parentDir not exist!
    protected abstract List<D> getDirs(D parentDir, GreatThanOrEquals gtOrEq, int fetchNum)
            throws S3ServerException;

    public DirCursor(String parentDir) throws S3ServerException {
        this.parentDir = parentDir;
    }

    public D getParentDirInfo() throws S3ServerException {
        if (parentDirInfo == null) {
            this.parentDirInfo = getDir(parentDir);
            if (parentDirInfo == null) {
                nomoreDir = true;
                return null;
            }
        }
        return parentDirInfo;
    }

    @Override
    public String toString() {
        return "parentDir:" + parentDir;
    }

    public String getParentDir() {
        return parentDir;
    }

    public boolean hasNext() throws S3ServerException {
        if (!dirs.isEmpty()) {
            return true;
        }

        if (nomoreDir) {
            return false;
        }
        if (parentDirInfo == null) {
            this.parentDirInfo = getDir(parentDir);
            if (parentDirInfo == null) {
                nomoreDir = true;
                return false;
            }
        }
        fetchMoreDir();

        if (!dirs.isEmpty()) {
            return true;
        }
        return false;
    }

    private void fetchMoreDir() throws S3ServerException {
        if (nomoreDir) {
            return;
        }

        List<D> dirlist = getDirs(parentDirInfo, new GreatThanOrEquals(dirGreaterThan, null),
                FETCH_NUM);
        if (dirlist == null) {
            nomoreDir = true;
            return;
        }
        if (dirlist.size() >= FETCH_NUM) {
            // 如果刚好是返回FETCH_NUM条，丢弃最后一条, 用于保证正常情况下，下次查询还能拿到数据
            dirlist.remove(dirlist.size() - 1);
        }
        else {
            nomoreDir = true;
            if (dirlist.size() == 0) {
                return;
            }
        }

        dirs.addAll(dirlist);
        D last = dirs.peekLast();
        if (last != null) {
            dirGreaterThan = getDirName(last);
        }
        if (dirlist.size() > 0) {
            Collections.sort(dirs, new Comparator<D>() {

                @Override
                public int compare(D o1, D o2) {
                    return (getDirName(o1) + "/").compareTo(getDirName(o2) + "/");
                }
            });
        }
    }

    protected abstract String getDirName(D dir);

    protected abstract FileDirInfo createFileInstanceByDir(D parent, D dir);

    public FileDirInfo getNext() throws S3ServerException {
        if (!hasNext()) {
            return null;
        }

        if (dirs.isEmpty()) {
            fetchMoreDir();
        }

        D dir = dirs.poll();
        if (dir == null) {
            return null;
        }
        return createFileInstanceByDir(parentDirInfo, dir);
    }

}
