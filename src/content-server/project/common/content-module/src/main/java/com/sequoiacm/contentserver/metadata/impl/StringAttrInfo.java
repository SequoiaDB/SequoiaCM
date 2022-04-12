package com.sequoiacm.contentserver.metadata.impl;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.contentserver.metadata.AttrRule;
import com.sequoiacm.contentserver.metadata.StringRule;

public class StringAttrInfo extends AttrInfoBase {
    private int maxLength = -1;
    private StringRule strRule;

    public StringAttrInfo(String name, boolean isRequired, AttrRule rule) {
        super(name, isRequired);
        this.strRule = (StringRule) rule;
        this.maxLength = strRule.getMaxLength();
    }
    
    @Override
    public AttributeType getType() {
        return AttributeType.STRING;
    }

    @Override
    public boolean check(Object o) {
        if (null == o) {
            return false;
        }

        if (!(o instanceof String)) {
            return false;
        }

        if (-1 == maxLength) {
            return true;
        }

        String v = (String) o;
        return v.length() <= maxLength;
    }

    @Override
    public String getRule() {
        return this.strRule.toStringFormat();
    }
}
