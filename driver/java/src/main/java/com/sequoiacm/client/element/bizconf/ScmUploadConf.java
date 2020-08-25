package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;

/**
 * Upload config class.
 */
public class ScmUploadConf {
    private boolean isOverwrite = false;
    private boolean isNeedMd5 = false;

    public ScmUploadConf(boolean isOverwrite) {
        this.isOverwrite = isOverwrite;
    }

    public ScmUploadConf(boolean isOverwrite, boolean isNeedMd5) {
        this.isOverwrite = isOverwrite;
        this.isNeedMd5 = isNeedMd5;
    }

    public boolean isOverwrite() {
        return isOverwrite;
    }

    public boolean isNeedMd5() {
        return isNeedMd5;
    }

    public void setNeedMd5(boolean needMd5) {
        isNeedMd5 = needMd5;
    }

    public void setOverwrite(boolean isOverwrite) {
        this.isOverwrite = isOverwrite;
    }

    public BSONObject toBsonObject() {
        BSONObject confBson = new BasicBSONObject();
        confBson.put(CommonDefine.RestArg.FILE_IS_OVERWRITE, isOverwrite);
        confBson.put(CommonDefine.RestArg.FILE_IS_NEED_MD5, isNeedMd5);
        return confBson;

    }
}
