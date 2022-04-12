package com.sequoiacm.contentserver.model;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

import com.sequoiacm.common.CommonDefine;

public class ClientUploadConf {
    private boolean isOverwrite = false;
    private boolean isNeedMd5 = false;

    public ClientUploadConf() {
    }

    public ClientUploadConf(boolean isOverwrite, boolean isNeedMd5) {
        this.isOverwrite = isOverwrite;
        this.isNeedMd5 = isNeedMd5;
    }

    public ClientUploadConf(BSONObject uploadConf) {
        if (uploadConf == null) {
            return;
        }
        if (uploadConf.containsField(CommonDefine.RestArg.FILE_IS_OVERWRITE)) {
            isOverwrite = (boolean) uploadConf.get(CommonDefine.RestArg.FILE_IS_OVERWRITE);
        }

        isNeedMd5 = BsonUtils.getBooleanOrElse(uploadConf, CommonDefine.RestArg.FILE_IS_NEED_MD5,
                false);
    }

    public boolean isOverwrite() {
        return isOverwrite;
    }

    public boolean isNeedMd5() {
        return isNeedMd5;
    }
}