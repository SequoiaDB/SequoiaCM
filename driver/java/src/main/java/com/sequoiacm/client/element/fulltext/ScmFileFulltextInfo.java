package com.sequoiacm.client.element.fulltext;

import org.bson.BSONObject;

import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;

/**
 * ScmFile fulltext information.
 */
public class ScmFileFulltextInfo {
    private ScmFileBasicInfo fileBasicInfo;
    private ScmFileFulltextStatus status;
    private String error;
    private String fulltextDocId;

    public ScmFileFulltextInfo(BSONObject fileBson) throws ScmException {
        fileBasicInfo = new ScmFileBasicInfo(fileBson);
        ScmFileFulltextExtData extData = new ScmFileFulltextExtData(
                BsonUtils.getBSON(fileBson, FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA));
        status = extData.getIdxStatus();
        error = extData.getErrorMsg();
        fulltextDocId = extData.getIdxDocumentId();
    }

    /**
     * Get the fulltext index document id.
     * @return id.
     */
    public String getFulltextDocId() {
        return fulltextDocId;
    }

    public void setFulltextDocId(String fulltextDocId) {
        this.fulltextDocId = fulltextDocId;
    }

    /**
     * Get the scm file basic information.
     * @return scm file basic information.
     */
    public ScmFileBasicInfo getFileBasicInfo() {
        return fileBasicInfo;
    }

    public void setFileBasicInfo(ScmFileBasicInfo fileBasicInfo) {
        this.fileBasicInfo = fileBasicInfo;
    }

    /**
     * Get the fulltext index status of the file. 
     * @return status.
     */
    public ScmFileFulltextStatus getStatus() {
        return status;
    }

    public void setStatus(ScmFileFulltextStatus status) {
        this.status = status;
    }

    /**
     * Get the fulltext index error message of the file. 
     * @return error message.
     */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ScmFileFulltextInfo [fileBasicInfo=" + fileBasicInfo + ", status=" + status
                + ", error=" + error + ", fulltextDocId=" + fulltextDocId + "]";
    }

}
