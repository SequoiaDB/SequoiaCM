package com.sequoiacm.contentserver.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.MetaDataDefine;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;

public class StringRule implements AttrRule {
    
    private int maxLength = -1;
    
    public StringRule(BSONObject rule) throws ScmServerException {
        if (rule == null) {
            rule = new BasicBSONObject();
        }
        
        for (String key : rule.keySet()) {
            if (!MetaDataDefine.CheckRuleName.MAX_LENGTH.equals(key)) {
                throw new ScmInvalidArgumentException("rule not supported by STRING type: " + key);
            }
            this.maxLength = convertObjToPositiveInt(key, rule.get(key));
        }
    }
    
    private int convertObjToPositiveInt(String key, Object val) throws ScmInvalidArgumentException {
        try {
            String strVal = String.valueOf(val);
            int len = Integer.parseInt(strVal);
            if (len < 0) {
                throw new ScmInvalidArgumentException("max length must be a positive");
            }
            return len;
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException("invalid rule value: " + key + "=" + val, e);
        }
    }

    public int getMaxLength() {
        return this.maxLength;
    }
    
    @Override
    public String toStringFormat() {
        return "String[maxLength:" + maxLength + "]";
    }
}
