package com.sequoiacm.tools.tag.common;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FileUpgradeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(FileUpgradeExecutor.class);

    private ThreadPoolExecutor threadPoolExecutor;
    private volatile FileFinishCallback fileFinishCallback;
    private Map<String, List<FileBasicInfo>> failedFile = new HashMap<>();
    private volatile int failedFileCount = 0;

    private AtomicLong activeTask = new AtomicLong(0);

    public FileUpgradeExecutor(int maxThread) {
        threadPoolExecutor = new ThreadPoolExecutor(maxThread, maxThread, 10, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void submit(WsBasicInfo ws, FileScope scope, BSONObject record) {
        threadPoolExecutor.submit(new FileTaskWithRecord(this, ws, scope, record));
        activeTask.incrementAndGet();
    }

    public void submit(WsBasicInfo ws, FileBasicInfo fileInfo) {
        threadPoolExecutor.submit(new FileTaskWithBasicInfo(this, ws, fileInfo));
        activeTask.incrementAndGet();
    }

    public synchronized void waitFinish() throws InterruptedException {
        while (true) {
            if (activeTask.get() <= 0) {
                return;
            }
            this.wait(5000);
        }
    }

    void notifyFinish(String ws, FileBasicInfo fileInfo, boolean success) {
        try {
            if (fileFinishCallback != null) {
                fileFinishCallback.onFileFinish(ws, fileInfo, success);
            }
            if (!success) {
                addFailedFile(ws, fileInfo);
            }
        }
        catch (Throwable e) {
            logger.error("notify finish error: ws={}, fileInfo={}, success={}", ws, fileInfo,
                    success, e);
        }
        finally {
            synchronized (this) {
                activeTask.decrementAndGet();
                this.notifyAll();
            }
        }
    }

    public void resetFileFinishCallback(FileFinishCallback callback) {
        this.fileFinishCallback = callback;
    }

    public void destroy() {
        try {
            threadPoolExecutor.shutdown();
            threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            logger.warn("failed to shutdown thread pool", e);
        }
    }

    public Map<String, List<FileBasicInfo>> getFailedFile() {
        if (failedFile.isEmpty()) {
            return Collections.emptyMap();
        }

        synchronized (this) {
            Map<String, List<FileBasicInfo>> ret = new HashMap<>();
            for (Map.Entry<String, List<FileBasicInfo>> e : failedFile.entrySet()) {
                ret.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            return ret;
        }
    }

    private synchronized void addFailedFile(String ws, FileBasicInfo fileInfo) {
        List<FileBasicInfo> failedFileList = failedFile.get(ws);
        if (failedFileList == null) {
            failedFileList = new ArrayList<>();
            failedFile.put(ws, failedFileList);
        }
        failedFileList.add(fileInfo);
        failedFileCount++;
    }

    public int getFailedFileCount() {
        return failedFileCount;
    }
}

class FileTaskWithBasicInfo implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FileTaskWithBasicInfo.class);
    private final FileUpgradeExecutor fileUpgradeExecutor;
    private final FileBasicInfo fileBasicInfo;
    private WsBasicInfo ws;

    public FileTaskWithBasicInfo(FileUpgradeExecutor fileUpgradeExecutor, WsBasicInfo ws,
            FileBasicInfo fileBasicInfo) {
        this.ws = ws;
        this.fileBasicInfo = fileBasicInfo;
        this.fileUpgradeExecutor = fileUpgradeExecutor;
    }

    @Override
    public void run() {
        try {
            BSONObject fileRecord = getFileRecord();
            if (fileRecord == null) {
                logger.info("file not found: ws=" + ws.getName() + ", file=" + fileBasicInfo);
                fileUpgradeExecutor.notifyFinish(ws.getName(), fileBasicInfo, true);
                return;
            }
            doTask(fileRecord);
            fileUpgradeExecutor.notifyFinish(ws.getName(), fileBasicInfo, true);
        }
        catch (Throwable e) {
            logger.error("failed to upgrade file: ws=" + ws.getName() + ", file=" + fileBasicInfo,
                    e);
            fileUpgradeExecutor.notifyFinish(ws.getName(), fileBasicInfo, false);
        }
    }

    protected BSONObject getFileRecord() throws ScmToolsException {
        return queryFileRecord(fileBasicInfo);
    }

    private BSONObject fileMatcher(BSONObject record) {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLFILE_ID, fileBasicInfo.getFileId());
        if (fileBasicInfo.getScope() == FileScope.HISTORY) {
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, fileBasicInfo.getMajorVersion());
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, fileBasicInfo.getMinorVersion());
        }
        matcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH,
                record.get(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH));
        return matcher;
    }

    private enum ListType {
        NUMBER,
        STRING,
        UNKNOWN;
    }

    private ListType detectListType(BasicBSONList list) {
        ListType ret = null;
        for (Object ele : list) {
            if (ele instanceof String) {
                if (ret == null || ret == ListType.STRING) {
                    ret = ListType.STRING;
                    continue;
                }
                return ListType.UNKNOWN;
            }
            if (ele instanceof Number) {
                if (ret == null || ret == ListType.NUMBER) {
                    ret = ListType.NUMBER;
                    continue;
                }
                return ListType.UNKNOWN;
            }
            return ListType.UNKNOWN;
        }
        return ret;
    }

    private void doTask(BSONObject fileRecord) throws Exception {
        List<TagName> tagNames = new ArrayList<>();

        boolean needUpdateTags = false;
        boolean needUpdateCustomTag = false;
        BasicBSONList tags = BsonUtils.getArray(fileRecord, FieldName.FIELD_CLFILE_TAGS);
        if (tags != null && !tags.isEmpty()) {
            ListType type = detectListType(tags);
            if (type == ListType.STRING) {
                for (Object tag : tags) {
                    tagNames.add(TagName.tags((String) tag));
                }
                needUpdateTags = true;
            }
            else if (type == ListType.UNKNOWN) {
                // 理论上不可能走到这
                throw new IllegalArgumentException("file tags is invalid:" + tags);
            }
            // type == Number，tags字段已经升级完毕（保存的是ID而非字符串）
        }

        Object customTagObj = BsonUtils.getObject(fileRecord, FieldName.FIELD_CLFILE_CUSTOM_TAG);
        if (customTagObj != null) {
            if (customTagObj instanceof BasicBSONObject) {
                BSONObject customTag = (BSONObject) customTagObj;
                for (String key : customTag.keySet()) {
                    tagNames.add(TagName.customTag(key, (String) customTag.get(key)));
                }
                needUpdateCustomTag = true;
            }
            else if (!(customTagObj instanceof BasicBSONList)) {
                // 理论上不可能走到这，要么是 BasicBSONObject （升级前）、要么是 BasicBSONList（升级后）
                throw new IllegalArgumentException("file custom tag is invalid:" + customTagObj);
            }
        }

        List<TagInfo> tagInfoList = TagLibMgr.getInstance().createTag(ws, tagNames);

        List<Long> tagsId = new ArrayList<>();
        List<Long> customTagId = new ArrayList<>();
        for (TagInfo tagInfo : tagInfoList) {
            if (tagInfo.getTagType() == TagType.TAGS) {
                tagsId.add(tagInfo.getTagId());
            }
            else if (tagInfo.getTagType() == TagType.CUSTOM_TAG) {
                customTagId.add(tagInfo.getTagId());
            }
            else {
                throw new IllegalArgumentException("unknown tag type: " + tagInfo);
            }
        }

        BasicBSONObject set = new BasicBSONObject();
        if (needUpdateTags) {
            set.put(FieldName.FIELD_CLFILE_TAGS, tagsId);
        }
        if (needUpdateCustomTag) {
            set.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, customTagId);
        }
        if (set.isEmpty()) {
            return;
        }
        BasicBSONObject updater = new BasicBSONObject("$set", set);
        BSONObject matcher = fileMatcher(fileRecord);
        update(fileBasicInfo.getScope().getFileClName(), updater, matcher);
    }

    private void update(String fileCl, BSONObject updater, BSONObject matcher)
            throws ScmToolsException {
        Sequoiadb sdb = SequoiadbDataSourceWrapper.getInstance().getConnection();
        try {
            sdb.getCollectionSpace(ws.getName() + "_META").getCollection(fileCl).update(matcher,
                    updater, null);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(sdb);
        }
    }

    private BSONObject queryFileRecord(FileBasicInfo fileBasicInfo) throws ScmToolsException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLFILE_ID, fileBasicInfo.getFileId());
        if (fileBasicInfo.getScope() == FileScope.HISTORY) {
            matcher.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, fileBasicInfo.getMajorVersion());
            matcher.put(FieldName.FIELD_CLFILE_MINOR_VERSION, fileBasicInfo.getMinorVersion());
        }
        ScmIdParser idParser = new ScmIdParser(fileBasicInfo.getFileId());
        matcher.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, idParser.getMonth());

        Sequoiadb sdb = SequoiadbDataSourceWrapper.getInstance().getConnection();
        try {
            return sdb.getCollectionSpace(ws.getName() + "_META")
                    .getCollection(fileBasicInfo.getScope().getFileClName())
                    .queryOne(matcher, null, null, null, 0);
        }
        finally {
            SequoiadbDataSourceWrapper.getInstance().releaseConnection(sdb);
        }
    }

}

class FileTaskWithRecord extends FileTaskWithBasicInfo {

    private final BSONObject fileRecord;

    public FileTaskWithRecord(FileUpgradeExecutor fileUpgradeExecutor, WsBasicInfo ws,
            FileScope scope, BSONObject fileRecord) {
        super(fileUpgradeExecutor, ws, new FileBasicInfo(scope, fileRecord));
        this.fileRecord = fileRecord;
    }

    @Override
    protected BSONObject getFileRecord() {
        return fileRecord;
    }
}

interface FileFinishCallback {
    void onFileFinish(String ws, FileBasicInfo fileInfo, boolean success);
}