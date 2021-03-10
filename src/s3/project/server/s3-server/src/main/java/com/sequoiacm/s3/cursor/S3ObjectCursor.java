package com.sequoiacm.s3.cursor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Stack;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ListObjRecord;
import com.sequoiacm.s3.model.Owner;
import com.sequoiacm.s3.remote.ScmContentServerClient;
import com.sequoiacm.s3.remote.ScmDirInfo;
import com.sequoiacm.s3.remote.ScmFileInfo;
import com.sequoiacm.s3.utils.CommonUtil;

public class S3ObjectCursor {
    private String prefix;
    private String startAfter;
    private String bucketDir;

    private Stack<FileDirCursor<?, ?>> cursorStack = new Stack<>();
    private ScmContentServerClient client;
    private boolean hasDelimiter;

    public S3ObjectCursor(ScmContentServerClient client, String prefix, String startAfter,
            String bucketDir, boolean hasDelimiter) throws S3ServerException {
        this.prefix = prefix;
        if (this.prefix == null) {
            this.prefix = S3CommonDefine.SCM_DIR_SEP;
        }
        this.startAfter = startAfter;
        if (this.startAfter == null) {
            this.startAfter = S3CommonDefine.SCM_DIR_SEP;
        }
        this.bucketDir = bucketDir;
        this.client = client;
        this.hasDelimiter = hasDelimiter;

        int res = this.startAfter.compareTo(this.prefix);
        if (res <= 0) {
            initStackByPrefix();
            return;
        }

        if (!startAfter.startsWith(this.prefix)) {
            return;
        }

        initStackByPrefixAndStartAfter();
    }

    private void initStackByPrefixAndStartAfter() throws S3ServerException {
        if (prefix.endsWith(S3CommonDefine.SCM_DIR_SEP)) {
            String p = prefix;
            // startAfter = '/a/b/c/d/das/dad/dasd/'
            // p = prefix = '/a/b/'
            // n = 'c/'
            while (true) {
                String gtName = getGreaterThanName(startAfter, p);
                ScmFileDirCursor cursor = new ScmFileDirCursor(client,
                        CommonUtil.concatPath(bucketDir, p), null, gtName);
                cursorStack.push(cursor);
                if (gtName == null) {
                    break;
                }
                if (hasDelimiter) {
                    break;
                }
                p = CommonUtil.concatPath(p, gtName);
            }
        }
        else {
            // startAfter = /a/b/namePrefix-XXX(GtName)/f/g
            // prefix = /a/b/namePrefix
            String parentDir = CommonUtil.dirname(prefix);
            String namePrefix = prefix.substring(parentDir.length());
            String gtName = getGreaterThanName(startAfter, parentDir);
            ScmFileDirCursor cursor = new ScmFileDirCursor(client,
                    CommonUtil.concatPath(bucketDir, parentDir), namePrefix, gtName);
            cursorStack.push(cursor);

            if (gtName != null && !hasDelimiter) {
                parentDir = CommonUtil.concatPath(parentDir, gtName);
                while (true) {
                    gtName = getGreaterThanName(startAfter, parentDir);
                    cursor = new ScmFileDirCursor(client,
                            CommonUtil.concatPath(bucketDir, parentDir), null, gtName);
                    cursorStack.push(cursor);
                    if (gtName == null) {
                        break;
                    }
                    parentDir = CommonUtil.concatPath(parentDir, gtName);
                }
            }
        }
    }

    private String getGreaterThanName(String path, String subPath) {
        if (!isSubPath(path, subPath)) {
            return null;
        }
        String tail = path.substring(subPath.length());
        if (tail.startsWith("/")) {
            tail = tail.substring(1, tail.length());
        }
        if (tail.contains("/")) {
            return tail.substring(0, tail.indexOf("/") + 1);
        }
        return tail;
    }

    private boolean isSubPath(String path, String subPath) {
        if (subPath.endsWith(S3CommonDefine.SCM_DIR_SEP)) {
            subPath = subPath.substring(0, subPath.length() - 1);
        }
        if (path.startsWith(subPath) && !path.equals(subPath)) {
            return true;
        }
        return false;
    }

    private void initStackByPrefix() throws S3ServerException {
        String parentDir = CommonUtil.concatPath(bucketDir, prefix);
        if (parentDir.endsWith(S3CommonDefine.SCM_DIR_SEP)) {
            // parentDir = /a/b/prefix/
            ScmFileDirCursor cursor = new ScmFileDirCursor(client, parentDir, null, null);
            cursorStack.push(cursor);
        }
        else {
            // parentDir = /a/b/prefix
            String namePrefix = CommonUtil.basename(parentDir);
            parentDir = parentDir.substring(0, parentDir.length() - namePrefix.length() - 1);
            ScmFileDirCursor cursor = new ScmFileDirCursor(client, parentDir, namePrefix, null);
            cursorStack.push(cursor);
        }
    }

    public ListObjRecord getNext(boolean fetchOwner, String encodingType) throws S3ServerException {
        while (!cursorStack.isEmpty()) {
            FileDirCursor<?, ?> cursor = cursorStack.peek();
            while (cursor.hasNext()) {
                FileDirInfo file = cursor.getNext();
                if (file.isDir()) {
                    if (!hasDelimiter) {
                        cursorStack
                                .push(new ScmFileDirCursor(client, file.getFullName(), null, null));
                        break;
                    }
                    if (!isEmpty(file)) {
                        return new ListObjRecord(
                                S3Codec.encode(file.getRelativePath(bucketDir), encodingType));
                    }
                    continue;
                }
                Owner owner = null;
                if (fetchOwner) {
                    owner = new Owner();
                    owner.setUserName(file.getUser());
                    owner.setUserId(file.getUser());
                }
                return new ListObjRecord(
                        S3Codec.encode(file.getRelativePath(bucketDir), encodingType),
                        file.getUpdateTime(), file.getEtag(), file.getSize(), owner);
            }
            if (cursorStack.peek() == cursor) {
                cursorStack.pop();
            }
        }

        return null;
    }

    private boolean isEmpty(FileDirInfo d) throws S3ServerException {
        // TODO: 这里的性能不是很好，看下还有没有其它更优的方案
        if (!d.isDir()) {
            throw new IllegalArgumentException("not dir:" + d.getFullName());
        }
        Stack<DirCursor<ScmDirInfo>> stack = new Stack<>();
        stack.push(new ScmDirCursor(client, d.getFullName()));
        while (!stack.isEmpty()) {
            DirCursor<ScmDirInfo> cursor = stack.peek();
            ScmDirInfo parent = cursor.getParentDirInfo();
            if (hasfile(parent)) {
                return false;
            }
            if (cursor.hasNext()) {
                FileDirInfo dir = cursor.getNext();
                if (!dir.isDir()) {
                    return false;
                }
                stack.push(new ScmDirCursor(client, dir.getFullName()));
                continue;
            }
            else {
                stack.pop();
            }
        }
        return true;
    }

    private boolean hasfile(ScmDirInfo parent) throws S3ServerException {
        if (parent == null) {
            return false;
        }
        List<ScmFileInfo> files;
        try {
            files = client.getFiles(parent.getId(), null, null, 1);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                return false;
            }
            throw new S3ServerException(S3Error.SCM_GET_FILE_FAILED,
                    "failed to list file:parent:" + parent.getId(), e);
        }
        if (files != null && files.size() > 0) {
            return true;
        }
        return false;
    }

    public boolean hasNext() throws S3ServerException {
        while (!cursorStack.isEmpty()) {
            FileDirCursor<?, ?> cursor = cursorStack.peek();
            if (cursor.hasNext()) {
                return true;
            }
            cursorStack.pop();
        }
        return false;
    }

}
