package com.sequoiacm.contentserver.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.MetaDataDefine;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;

public class IntegerRule implements AttrRule {
    
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;
    
    public IntegerRule(BSONObject rule) throws ScmServerException {
        if (rule == null) {
            rule = new BasicBSONObject();
        }
        
        for (String key : rule.keySet()) {
            if (MetaDataDefine.CheckRuleName.MIN.equals(key)) {
                min = convertObjToInt(key, rule.get(key));
            }
            else if (MetaDataDefine.CheckRuleName.MAX.equals(key)) {
                max = convertObjToInt(key, rule.get(key));
            }
            else {
                throw new ScmInvalidArgumentException("rule not supported by INTEGER type: " + key);
            }
        }
        
        if (min > max) {
            throw new ScmInvalidArgumentException("invalid integer range: min=" + min + ",max="
                    + max);
        }
    }
    
    private int convertObjToInt(String key, Object val) throws ScmInvalidArgumentException {
        try {
            String strVal = String.valueOf(val);
            return Integer.parseInt(strVal);
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException("invalid integer value: " + key + "=" + val, e);
        }
    }

    public int getMinimum() {
        return this.min;
    }
    
    public int getMaximum() {
        return this.max;
    }
    
    @Override
    public String toStringFormat() {
        return "Integer[" + min + "," + max + "]";
    }
}
