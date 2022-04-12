package com.sequoiacm.contentserver.metadata.impl;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metadata.AttrRule;
import com.sequoiacm.contentserver.metadata.DoubleRule;

public class DoubleAttrInfo extends AttrInfoBase {
    private Double min = Double.MIN_VALUE;
    private Double max = Double.MAX_VALUE;
    private DoubleRule doubleRule;

    public DoubleAttrInfo(String name, boolean isRequired, AttrRule rule)
            throws ScmServerException {
        super(name, isRequired);
        this.doubleRule = (DoubleRule) rule;
        this.min = doubleRule.getMinimum();
        this.max = doubleRule.getMaximum();
    }
    
    @Override
    public AttributeType getType() {
        return AttributeType.DOUBLE;
    }

    @Override
    public boolean check(Object o) {
        if (null == o) {
            return false;
        }

        if (!(o instanceof Double)) {
            return false;
        }

        double d = (double) o;
        return d >= min && d <= max;
    }

    @Override
    public String getRule() {
        return this.doubleRule.toStringFormat();
    }
}
