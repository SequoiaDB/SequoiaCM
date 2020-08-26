package com.sequoiacm.infrastructure.fulltext.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;

public class ScmFileFulltextExtData {
    public static final String FIELD_IDX_DOC_ID = "fulltext_document_id";
    public static final String FIELD_IDX_STATUS = "fulltext_status";
    public static final String FIELD_ERROR_MSG = "fulltext_error";
    private String idxDocumentId;
    private ScmFileFulltextStatus idxStatus = ScmFileFulltextStatus.NONE;
    private String errorMsg;

    public ScmFileFulltextExtData() {
    }

    public ScmFileFulltextExtData(String idxDocumentId, ScmFileFulltextStatus idxStatus,
            String errorMsg) {
        this.idxDocumentId = idxDocumentId;
        this.idxStatus = idxStatus;
        this.errorMsg = errorMsg;
    }

    public ScmFileFulltextExtData(BSONObject externalData) {
        if (externalData == null) {
            return;
        }
        idxDocumentId = BsonUtils.getString(externalData, FIELD_IDX_DOC_ID);
        idxStatus = ScmFileFulltextStatus
                .valueOf(BsonUtils.getString(externalData, FIELD_IDX_STATUS));
        errorMsg = BsonUtils.getString(externalData, FIELD_ERROR_MSG);
    }

    public BSONObject toBson() {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FIELD_IDX_STATUS, idxStatus.name());
        ret.put(FIELD_IDX_DOC_ID, idxDocumentId);
        ret.put(FIELD_ERROR_MSG, errorMsg);
        return ret;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public String getIdxDocumentId() {
        return idxDocumentId;
    }

    public ScmFileFulltextStatus getIdxStatus() {
        return idxStatus;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public void setIdxDocumentId(String idxDocumentId) {
        this.idxDocumentId = idxDocumentId;
    }

    public void setIdxStatus(ScmFileFulltextStatus idxStatus) {
        this.idxStatus = idxStatus;
    }
}
