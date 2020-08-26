package com.sequoiacm.client.element.fulltext;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;

public class ScmFulltextOption {
    private BSONObject fileCondition;
    private ScmFulltextMode mode;

    public ScmFulltextOption(BSONObject fileCondition, ScmFulltextMode mode) throws ScmException {
        setFileCondition(fileCondition);
        setMode(mode);
    }

    public BSONObject getFileCondition() {
        return fileCondition;
    }

    public ScmFulltextMode getMode() {
        return mode;
    }

    public void setMode(ScmFulltextMode mode) throws ScmException {
        if (mode == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "mode is null");
        }
        this.mode = mode;
    }

    public void setFileCondition(BSONObject fileCondition) throws ScmException {
        if (fileCondition == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "fileCondition is null");
        }
        this.fileCondition = fileCondition;
    }
}
