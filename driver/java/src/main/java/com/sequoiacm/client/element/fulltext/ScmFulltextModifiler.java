package com.sequoiacm.client.element.fulltext;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;

/**
 * Workspace fulltext index option modifier. 
 */
public class ScmFulltextModifiler {
    private BSONObject newFileCondition;
    private ScmFulltextMode newMode;

    /**
     * Reset fulltext mode. 
     * @param mode
     *         mode.
     * @return ${code this}
     * @throws ScmInvalidArgumentException
     *          if error happens. 
     */
    public ScmFulltextModifiler newMode(ScmFulltextMode mode) throws ScmInvalidArgumentException {
        if (mode == null) {
            throw new ScmInvalidArgumentException("mode is null");
        }
        this.newMode = mode;
        return this;
    }

    /**
     * Reset workspace fulltext index file coondition.
     * @param fileCondition
     *          file condition.
     * @return ${code this}
     */
    public ScmFulltextModifiler newFileCondition(BSONObject fileCondition) {
        this.newFileCondition = fileCondition;
        return this;
    }

    /**
     * Get the file condition for reset workspace.
     * @return
     */
    public BSONObject getNewFileCondition() {
        return newFileCondition;
    }

    /**
     * Get the mode for reset workspace.
     * @return
     */
    public ScmFulltextMode getNewMode() {
        return newMode;
    }

}
