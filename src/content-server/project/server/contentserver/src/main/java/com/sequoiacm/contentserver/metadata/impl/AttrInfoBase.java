package com.sequoiacm.contentserver.metadata.impl;

import com.sequoiacm.contentserver.metadata.AttrInfo;

public abstract class AttrInfoBase implements AttrInfo {
    private String name;
    private boolean isRequired;

    public AttrInfoBase(String name, boolean isRequired) {
        this.name = name;
        this.isRequired = isRequired;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }
}
