package com.sequoiacm.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

/**
 * Option for scm file update content.
 */
public class ScmUpdateContentOption {
    private static final String KEY_IS_NEED_MD5 = "is_need_md5";
    private boolean isNeedMd5 = false;

    /**
     *  Create a new instance with default option.
     */
    public ScmUpdateContentOption() {
    }

    public ScmUpdateContentOption(BSONObject bson) {
        if (bson == null) {
            return;
        }
        isNeedMd5 = BsonUtils.getBooleanOrElse(bson, KEY_IS_NEED_MD5, isNeedMd5);
    }

    /**
     * Create a new instance.
     * @param isNeedMd5  
     *          if need calculate md5 for new file content.
     */
    public ScmUpdateContentOption(boolean isNeedMd5) {
        this.isNeedMd5 = isNeedMd5;
    }

    /**
     * Return if need calculate md5 for new file content.
     * @return is need calculate md5.
     */
    public boolean isNeedMd5() {
        return isNeedMd5;
    }

    /**
     * Set need calculate md5 for new file content.
     * @param isNeedMd5 
     *          is need calculate md5.
     */
    public void setNeedMd5(boolean isNeedMd5) {
        this.isNeedMd5 = isNeedMd5;
    }

    public BSONObject toBson() {
        return new BasicBSONObject(KEY_IS_NEED_MD5, isNeedMd5);
    }

    @Override
    public String toString() {
        return "ScmUpdateContentOption [isNeedMd5=" + isNeedMd5 + "]";
    }

}
