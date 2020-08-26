package com.sequoiacm.contentserver.metadata;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;

public class BooleanRule implements AttrRule {
    
    public BooleanRule(BSONObject rule) throws ScmServerException {
        if (rule != null && !rule.isEmpty()) {
            throw new ScmInvalidArgumentException(
                    "BOOLEAN type does not need to specify rules");
        }
    }
    
    @Override
    public String toStringFormat() {
        return "Boolean[true|false]";
    }
}
