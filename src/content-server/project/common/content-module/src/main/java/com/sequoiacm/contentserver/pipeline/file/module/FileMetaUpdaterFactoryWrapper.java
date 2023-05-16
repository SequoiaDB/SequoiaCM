package com.sequoiacm.contentserver.pipeline.file.module;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.dao.ScmFileVersionHelper;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.model.UpdaterKeyDefine;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.contentserver.tag.TagLibMgr;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FileMetaUpdaterFactoryWrapper {
    @Autowired
    private List<CustomFileMetaUpdaterFactory> customFileMetaUpdaterFactories;
    private DefaultFileMetaUpdaterFactory defaultFileMetaUpdaterFactory = new DefaultFileMetaUpdaterFactory();

    public FileMetaUpdater createFileMetaUpdater(ScmWorkspaceInfo ws, String clientUpdaterKey,
            Object clientUpdaterValue, int majorVersion, int minorVersion)
            throws ScmServerException {
        for (CustomFileMetaUpdaterFactory customFileMetaUpdaterFactory : customFileMetaUpdaterFactories) {
            if (customFileMetaUpdaterFactory.isSupported(clientUpdaterKey)) {
                return customFileMetaUpdaterFactory.createFileMetaUpdater(ws, clientUpdaterKey,
                        clientUpdaterValue, majorVersion, minorVersion);
            }
        }
        return defaultFileMetaUpdaterFactory.createFileMetaUpdater(clientUpdaterKey,
                clientUpdaterValue, majorVersion, minorVersion);
    }
}

class DefaultFileMetaUpdaterFactory {
    public FileMetaUpdater createFileMetaUpdater(String clientUpdaterKey, Object clientUpdaterValue,
            int majorVersion, int minorVersion) throws ScmInvalidArgumentException {
        boolean isGlobal = ScmFileVersionHelper.isUnifiedField(clientUpdaterKey);
        if (isGlobal) {
            return FileMetaDefaultUpdater.globalFieldUpdater(clientUpdaterKey, clientUpdaterValue);
        }
        return FileMetaDefaultUpdater.versionFieldUpdater(clientUpdaterKey, clientUpdaterValue,
                majorVersion, minorVersion);
    }
}

interface CustomFileMetaUpdaterFactory {
    FileMetaUpdater createFileMetaUpdater(ScmWorkspaceInfo ws, String clientUpdaterKey,
            Object clientUpdaterValue, int majorVersion, int minorVersion)
            throws ScmServerException;

    // 实现类需要注意，不能存在两个 factory 都支持同一个 key 的情况
    boolean isSupported(String clientUpdaterKey);

}

@Component
class FileMetaTagSetUpdaterFactory implements CustomFileMetaUpdaterFactory {
    @Autowired
    private TagLibMgr tagLibMgr;

    @Override
    public FileMetaUpdater createFileMetaUpdater(ScmWorkspaceInfo ws, String clientUpdaterKey,
            Object clientUpdaterValue, int majorVersion, int minorVersion)
            throws ScmServerException {

        if (clientUpdaterKey.equals(FieldName.FIELD_CLFILE_TAGS)) {
            List<TagName> tags = new ArrayList<>();
            BasicBSONList clientUpdaterValueBson = (BasicBSONList) clientUpdaterValue;
            for (Object tag : clientUpdaterValueBson) {
                tags.add(TagName.tags((String) tag));
            }
            if (!ws.newVersionTag()) {
                return new FileMetaSetTagUpdaterV1(TagType.TAGS, tags, majorVersion, minorVersion);
            }

            // 在这里进行标签创建，之所以没有延后到 FileMetaSetTagUpdaterV2.injectFileUpdater 中创建是因为该函数调用时处于
            // 文件更新的 pipeline 中，该 pipeline 会持有文件锁，并且位于 SDB 事务中，
            // 而标签创建逻辑内部本身会需要标签锁、数据库操作，放入其中会放大文件锁的范围、增加事务的时常。
            List<TagInfo> tagInfoList = tagLibMgr.createTag(ws, tags);

            return new FileMetaSetTagUpdaterV2(TagType.TAGS, tagInfoList, majorVersion,
                    minorVersion);
        }
        else if (clientUpdaterKey.equals(FieldName.FIELD_CLFILE_CUSTOM_TAG)) {
            List<TagName> tags = new ArrayList<>();
            BSONObject clientUpdaterValueBson = (BSONObject) clientUpdaterValue;
            for (String key : clientUpdaterValueBson.keySet()) {
                tags.add(TagName.customTag(key, (String) clientUpdaterValueBson.get(key)));
            }
            if (!ws.newVersionTag()) {
                return new FileMetaSetTagUpdaterV1(TagType.CUSTOM_TAG, tags, majorVersion, minorVersion);
            }

            List<TagInfo> tagInfoList = tagLibMgr.createTag(ws, tags);
            return new FileMetaSetTagUpdaterV2(TagType.CUSTOM_TAG, tagInfoList, majorVersion,
                    minorVersion);
        }
        throw new ScmInvalidArgumentException("invalid clientUpdaterKey: " + clientUpdaterKey);
    }

    @Override
    public boolean isSupported(String clientUpdaterKey) {
        return clientUpdaterKey.equals(FieldName.FIELD_CLFILE_TAGS)
                || clientUpdaterKey.equals(FieldName.FIELD_CLFILE_CUSTOM_TAG);
    }
}

@Component
class FileMetaTagAddUpdaterFactory implements CustomFileMetaUpdaterFactory {

    @Autowired
    private TagLibMgr tagLibMgr;

    @Override
    public FileMetaUpdater createFileMetaUpdater(ScmWorkspaceInfo ws, String clientUpdaterKey,
            Object clientUpdaterValue, int majorVersion, int minorVersion)
            throws ScmServerException {
        if (!ws.newVersionTag()) {
            // 旧版工作区不支持标签增量更新
            throw new ScmOperationUnsupportedException(
                    "the workspace unsupported tag add: " + ws.getName());
        }
        if (clientUpdaterKey.equals(UpdaterKeyDefine.ADD_CUSTOM_TAG)) {
            BSONObject clientUpdaterValueBson = (BSONObject) clientUpdaterValue;
            String tagTey = clientUpdaterValueBson.keySet().iterator().next();
            String tagValue = (String) clientUpdaterValueBson.get(tagTey);
            // 在这里进行标签创建，之所以没有延后到 FileMetaAddTagUpdater.injectFileUpdater 中创建是因为该函数调用时处于
            // 文件更新的 pipeline 中，该 pipeline 会持有文件锁，并且位于 SDB 事务中，
            // 而标签创建逻辑内部本身会需要标签锁、数据库操作，放入其中会放大文件锁的范围、增加事务的时常。
            TagInfo tagInfo = tagLibMgr.createTag(ws, TagName.customTag(tagTey, tagValue));
            return new FileMetaAddTagUpdater(tagInfo, majorVersion, minorVersion);
        }
        else if (clientUpdaterKey.equals(UpdaterKeyDefine.ADD_TAG)) {
            TagInfo tagInfo = tagLibMgr.createTag(ws, TagName.tags((String) clientUpdaterValue));
            return new FileMetaAddTagUpdater(tagInfo, majorVersion, minorVersion);
        }

        throw new ScmInvalidArgumentException("Unsupported clientUpdaterKey: " + clientUpdaterKey);
    }

    @Override
    public boolean isSupported(String clientUpdaterKey) {
        return clientUpdaterKey.equals(UpdaterKeyDefine.ADD_CUSTOM_TAG)
                || clientUpdaterKey.equals(UpdaterKeyDefine.ADD_TAG);
    }
}

@Component
class FileMetaTagRemoveUpdaterFactory implements CustomFileMetaUpdaterFactory {

    @Autowired
    private TagLibMgr tagLibMgr;

    @Override
    public FileMetaUpdater createFileMetaUpdater(ScmWorkspaceInfo ws, String clientUpdaterKey,
            Object clientUpdaterValue, int majorVersion, int minorVersion)
            throws ScmServerException {
        if (!ws.newVersionTag()) {
            // 旧版工作区不支持标签增量更新
            throw new ScmOperationUnsupportedException(
                    "the workspace unsupported tag remove: " + ws.getName());
        }
        if (clientUpdaterKey.equals(UpdaterKeyDefine.REMOVE_CUSTOM_TAG)) {
            BSONObject clientUpdaterValueBson = (BSONObject) clientUpdaterValue;
            String tagTey = clientUpdaterValueBson.keySet().iterator().next();
            String tagValue = (String) clientUpdaterValueBson.get(tagTey);
            TagInfo tagInfo = tagLibMgr.getTagInfo(ws, TagName.customTag(tagTey, tagValue));
            return new FileMetaRemoveTagUpdater(tagInfo, majorVersion, minorVersion);
        }
        else if (clientUpdaterKey.equals(UpdaterKeyDefine.REMOVE_TAG)) {
            TagInfo tagInfo = tagLibMgr.getTagInfo(ws, TagName.tags((String) clientUpdaterValue));
            return new FileMetaRemoveTagUpdater(tagInfo, majorVersion, minorVersion);
        }

        throw new ScmInvalidArgumentException("Unsupported clientUpdaterKey: " + clientUpdaterKey);
    }

    @Override
    public boolean isSupported(String clientUpdaterKey) {
        return clientUpdaterKey.equals(UpdaterKeyDefine.REMOVE_CUSTOM_TAG)
                || clientUpdaterKey.equals(UpdaterKeyDefine.REMOVE_TAG);
    }
}