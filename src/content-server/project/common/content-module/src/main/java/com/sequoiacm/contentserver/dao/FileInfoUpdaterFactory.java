package com.sequoiacm.contentserver.dao;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

public class FileInfoUpdaterFactory {
    // newFileInfo key 为要更新的文件属性，value 为新的值
    // 需要在锁内使用 FileInfoUpdater
    // 调用者需要自行保证 key、value 的有效性
    public static FileInfoUpdater create(BSONObject newFileInfo) throws ScmServerException {
        if (isUpdateUnifiedProp(newFileInfo)) {
            // 表示更新的是所有版本一致的文件属性，如文件名、目录ID ，这些属性定义在 ScmFileVersionHelper.UNIFIED_FIELD
            return new UnifiedInfoUpdater();
        }
        // 表示更新的是各个版本独有属性，如作者，除 ScmFileVersionHelper.UNIFIED_FIELD 以外的属性，都认为是各个版本独有的文件属性
        return new NotUnifiedInfoUpdater();
    }

    private static boolean isUpdateUnifiedProp(BSONObject fileUpdater) throws ScmServerException {
        boolean hasUnifiedField = false;
        boolean hasNotUnifiedField = false;
        for (String key : fileUpdater.keySet()) {
            if (ScmFileVersionHelper.isUnifiedField(key)) {
                hasUnifiedField = true;
            }
            else {
                hasNotUnifiedField = true;
            }
        }
        if (hasNotUnifiedField && hasUnifiedField) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "update contain unified field and no unified field:" + fileUpdater.keySet());
        }
        if (hasUnifiedField) {
            return true;
        }
        return false;
    }
}