package com.sequoiacm.contentserver.metadata;

import com.sequoiacm.common.AttributeType;

public interface AttrInfo {
    AttributeType getType();

    String getName();

    boolean isRequired();

    boolean check(Object o);

    String getRule();
}
