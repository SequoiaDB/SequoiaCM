package com.sequoiacm.contentserver.metadata.impl;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.metadata.AttrRule;
import com.sequoiacm.contentserver.metadata.IntegerRule;

public class IntegerAttrInfo extends AttrInfoBase {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;
    private IntegerRule intRule;

    public IntegerAttrInfo(String name, boolean isRequired, AttrRule rule)
            throws ScmServerException {
        super(name, isRequired);
        this.intRule = (IntegerRule) rule;
        this.min = intRule.getMinimum();
        this.max = intRule.getMaximum();
    }
    
    @Override
    public AttributeType getType() {
        return AttributeType.INTEGER;
    }

    @Override
    public boolean check(Object o) {
        if (null == o) {
            return false;
        }

        if (!(o instanceof Integer)) {
            return false;
        }

        int i = (int) o;
        return i >= min && i <= max;
    }

    @Override
    public String getRule() {
        return this.intRule.toStringFormat();
    }
}