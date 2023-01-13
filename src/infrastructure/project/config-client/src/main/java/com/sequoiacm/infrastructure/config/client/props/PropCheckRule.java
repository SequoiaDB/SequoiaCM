package com.sequoiacm.infrastructure.config.client.props;

public interface PropCheckRule {
    boolean checkValue(String value);

    boolean isDeletable();
}
