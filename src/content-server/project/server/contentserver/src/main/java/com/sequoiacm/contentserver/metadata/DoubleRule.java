package com.sequoiacm.contentserver.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.MetaDataDefine;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;

public class DoubleRule implements AttrRule {
    
    private double min = Double.MIN_VALUE;
    private double max = Double.MAX_VALUE;
    
    public DoubleRule(BSONObject rule) throws ScmServerException {
        if (rule == null) {
            rule = new BasicBSONObject();
        }
        
        for (String key : rule.keySet()) {
            if (MetaDataDefine.CheckRuleName.MIN.equals(key)) {
                min = convertObjToDouble(key, rule.get(key));
            }
            else if (MetaDataDefine.CheckRuleName.MAX.equals(key)) {
                max = convertObjToDouble(key, rule.get(key));
            }
            else {
                throw new ScmInvalidArgumentException("rule not supported by DOUBLE type: " + key);
            }
        }
        
        if (min > max) {
            throw new ScmInvalidArgumentException("invalid double range: min=" + min + ",max="
                    + max);
        }
    }
    
    private double convertObjToDouble(String key, Object val) throws ScmInvalidArgumentException {
        try {
            String strVal = String.valueOf(val);
            return Double.parseDouble(strVal);
        }
        catch (Exception e) {
            throw new ScmInvalidArgumentException("invalid double value: " + key + "=" + val, e);
        }
    }

    public double getMinimum() {
        return this.min;
    }
    
    public double getMaximum() {
        return this.max;
    }
    
    @Override
    public String toStringFormat() {
        return "Double[" + min + "," + max + "]";
    }
}
