package com.sequoiacm.contentserver.metadata.impl;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.contentserver.metadata.AttrRule;
import com.sequoiacm.contentserver.metadata.BooleanRule;

public class BooleanAttrInfo extends AttrInfoBase {

    private BooleanRule boolRule;
    
    public BooleanAttrInfo(String name, boolean isRequired, AttrRule rule) {
        super(name, isRequired);
        this.boolRule = (BooleanRule) rule;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.BOOLEAN;
    }
    
    @Override
    public boolean check(Object o) {
        if (null == o) {
            return false;
        }

        if (o instanceof Boolean) {
            return true;
        }

        if (o instanceof String) {
            if ("true".equalsIgnoreCase((String) o) || "false".equalsIgnoreCase((String) o)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getRule() {
        return this.boolRule.toStringFormat();
    }
}
