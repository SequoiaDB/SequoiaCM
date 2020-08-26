package com.sequoiacm.contentserver.metadata;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;

public class DateRule implements AttrRule {
    
    private String dateformat = "yyyy-MM-dd-HH:mm:ss.SSS";
    
    public DateRule(BSONObject rule) throws ScmServerException {
        if (rule != null && !rule.isEmpty()) {
            throw new ScmInvalidArgumentException(
                    "DATE type does not need to specify rules");
        }
    }
    
    public String getFormat() {
        return this.dateformat;
    }
    
    @Override
    public String toStringFormat() {
        return "Date[" + dateformat + "]";
    }
}
