package com.sequoiacm.s3.cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.remote.GreatThanOrEquals;

public abstract class FileDirCursor<F, D> {
    private static final int FETCH_NUM = 50;
    private String parentDir;
    private D parentDirInfo;
    private String startWith;
    private LinkedList<F> files = new LinkedList<>();
    private LinkedList<D> dirs = new LinkedList<>();
    private int unsortedDirIdx;
    private String fileGreaterThan;
    private String dirGreaterThan;
    private boolean nomoreDir;
    private boolean nomoreFile;
    private List<String> dirEqualsList;

    // return null if path not exist!
    protected abstract D getDir(String path) throws S3ServerException;

    // return null if parentDir not exist!
    protected abstract List<D> getDirs(D parentDir, String startWith, GreatThanOrEquals gtOrEq,
            int fetchNum) throws S3ServerException;

    // return null if parentDir not exist!
    protected abstract List<F> getFiles(D parentDir, String startWith, String greaterThan,
            int fetchNum) throws S3ServerException;

    public FileDirCursor(String parentDir, String startWith, String greaterThan)
            throws S3ServerException {
        this.parentDir = parentDir;
        this.startWith = startWith;
        this.dirGreaterThan = greaterThan;
        this.fileGreaterThan = greaterThan;

        this.dirEqualsList = getEqualsFromGreaterThan(greaterThan);
        /* dirGreaterThan = aa.bb.cc 
         * dirEqualsList = aa, aa.bb 
         * 目录查询条件： 
                {
                    $and: [{
                        parentId: 'parentDir_id'
                    },
                    {
                        name: {$reg: '^prefix'}
                    },
                    {
                        $or: [{
                            name: {$gt: 'aa.bb.cc'}
                        },
                        {
                            name: {$in: ['aa', 'aa.bb']}
                        }]
                    }]
                }
         */
    }

    private List<String> getEqualsFromGreaterThan(String greaterThan) {
        if (greaterThan == null) {
            return null;
        }

        List<String> ret = new ArrayList<>();
        for (int i = 0; i < greaterThan.length(); i++) {
            if (greaterThan.charAt(i) < S3CommonDefine.SCM_DIR_SEP_CHAR) {
                String eq = greaterThan.substring(0, i);
                if (!eq.isEmpty()) {
                    ret.add(eq);
                }
            }
        }

        return ret;
    }

    @Override
    public String toString() {
        return "parentDir:" + parentDir;
    }

    public String getParentDir() {
        return parentDir;
    }

    public boolean hasFile() throws S3ServerException {
        if (hasNext()) {
            return !files.isEmpty();
        }
        return false;
    }

    public boolean hasNext() throws S3ServerException {
        if (!files.isEmpty() || !dirs.isEmpty()) {
            return true;
        }

        if (nomoreDir && nomoreFile) {
            return false;
        }

        if (parentDirInfo == null) {
            parentDirInfo = getDir(parentDir);
            if (parentDirInfo == null) {
                nomoreDir = true;
                nomoreFile = true;
                return false;
            }
        }

        fetchMoreFile();
        fetchMoreDir();

        if (!files.isEmpty() || !dirs.isEmpty()) {
            return true;
        }

        return false;
    }

    private void fetchMoreDir() throws S3ServerException {
        // unsortedDirIdx 表示 dirs 中，从该索引值开始的目录顺序是待决的：
        // 假设 parentDir 下有如下目录： AA BB BB. BB.. BB... FF
        // 按名字排序一次获取 4 条 ： AA BB BB. BB..
        // 本地为名字末尾拼上 '/' 再进行一次排序，dirs中的数据为
        // 那么此时 unsortedDirIdx 的取值必须是 1 ，即 BB.. BB. BB 顺序是待决的，
        // 就是为了防止下一条数据出现 BB... （BB... 比 BB.. 要小）
        if (nomoreDir) {
            unsortedDirIdx = dirs.size();
            return;
        }
        while (unsortedDirIdx == 0 && !nomoreDir) {
            List<D> dirlist = getDirs(parentDirInfo, startWith,
                    new GreatThanOrEquals(dirGreaterThan, dirEqualsList), FETCH_NUM);
            if (dirlist == null) {
                nomoreDir = true;
                unsortedDirIdx = dirs.size();
                return;
            }
            if (dirlist.size() >= FETCH_NUM) {
                // 如果刚好是返回FETCH_NUM条，丢弃最后一条, 用于保证正常情况下，下次查询还能拿到数据
                dirlist.remove(dirlist.size() - 1);
            }
            else {
                nomoreDir = true;
                if (dirlist.size() == 0) {
                    unsortedDirIdx = dirs.size();
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
            unsortedDirIdx = calculateUnsortedIndex();
        }
    }

    protected abstract String getDirName(D dir);

    protected abstract String getFileName(F dir);

    protected abstract FileDirInfo createFileInstanceByDir(D parent, D dir);

    protected abstract FileDirInfo createFileInstanceByFile(D parent, F file);

    private int calculateUnsortedIndex() {
        if (nomoreDir) {
            return dirs.size();
        }
        D lastDir = dirs.peekLast();
        unsortedDirIdx = dirs.size() - 1;
        ListIterator<D> it = dirs.listIterator(dirs.size() - 1);
        while (it.hasPrevious()) {
            int previousIdx = it.previousIndex();
            D previous = it.previous();
            if (!getDirName(previous).startsWith(getDirName(lastDir))) {
                break;
            }
            unsortedDirIdx = previousIdx;
        }
        return unsortedDirIdx;
    }

    public FileDirInfo getNext() throws S3ServerException {
        if (!hasNext()) {
            return null;
        }

        if (files.isEmpty()) {
            fetchMoreFile();
        }

        if (dirs.isEmpty() || unsortedDirIdx == 0) {
            fetchMoreDir();
        }

        D dir = dirs.peek();
        F file = files.peek();
        if (file == null && dir == null) {
            return null;
        }
        if (dir == null) {
            return createFileInstanceByFile(parentDirInfo, files.poll());
        }
        if (file == null) {
            unsortedDirIdx--;
            return createFileInstanceByDir(parentDirInfo, dirs.poll());
        }

        if (getFileName(file).compareTo(getDirName(dir) + "/") < 0) {
            return createFileInstanceByFile(parentDirInfo, files.poll());
        }
        unsortedDirIdx--;
        return createFileInstanceByDir(parentDirInfo, dirs.poll());
    }

    private void fetchMoreFile() throws S3ServerException {
        if (nomoreFile) {
            return;
        }
        List<F> fileList = getFiles(parentDirInfo, startWith, fileGreaterThan, FETCH_NUM);
        if (fileList == null) {
            nomoreFile = true;
            nomoreDir = true;
            return;
        }

        if (fileList.size() >= FETCH_NUM) {
            // 如果刚好是返回FETCH_NUM条，丢弃最后一条, 用于保证正常情况下，下次查询还能拿到数据
            fileList.remove(fileList.size() - 1);
        }
        else {
            nomoreFile = true;
        }

        files.addAll(fileList);
        F last = files.peekLast();
        if (last != null) {
            fileGreaterThan = getFileName(last);
        }
    }

}
