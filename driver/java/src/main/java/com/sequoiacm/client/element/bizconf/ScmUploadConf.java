package com.sequoiacm.client.element.bizconf;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;

/**
 * 
 * Upload config class.
 *
 */
public class ScmUploadConf {
    private boolean isOverwrite = false;

    public ScmUploadConf(boolean isOverwrite) {
        this.isOverwrite = isOverwrite;
    }

    public boolean isOverwrite() {
        return isOverwrite;
    }

    public void setOverwrite(boolean isOverwrite) {
        this.isOverwrite = isOverwrite;
    }

    public BSONObject toBsonObject() {
        BSONObject confBson = new BasicBSONObject();
        confBson.put(CommonDefine.RestArg.FILE_IS_OVERWRITE, isOverwrite);
        return confBson;

    }
}
