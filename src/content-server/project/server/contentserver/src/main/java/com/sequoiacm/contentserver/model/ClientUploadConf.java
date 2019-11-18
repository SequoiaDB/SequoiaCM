package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;

import com.sequoiacm.common.CommonDefine;

public class ClientUploadConf {
    private boolean isOverwrite = false;

    public ClientUploadConf() {
    }

    public ClientUploadConf(BSONObject uploadConf) {
        if (uploadConf == null) {
            return;
        }
        if (uploadConf.containsField(CommonDefine.RestArg.FILE_IS_OVERWRITE)) {
            isOverwrite = (boolean) uploadConf.get(CommonDefine.RestArg.FILE_IS_OVERWRITE);
        }
    }

    public boolean isOverwrite() {
        return isOverwrite;
    }

}