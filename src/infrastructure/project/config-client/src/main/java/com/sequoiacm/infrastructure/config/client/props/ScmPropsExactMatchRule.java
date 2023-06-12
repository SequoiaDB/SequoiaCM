package com.sequoiacm.infrastructure.config.client.props;

import java.util.Objects;

public class ScmPropsExactMatchRule implements ScmPropsMatchRule {
    private final String conf;

    public ScmPropsExactMatchRule(String conf) {
        this.conf = conf;
    }

    @Override
    public boolean isMatch(String confProp) {
        return conf.equals(confProp);
    }

    @Override
    public String toString() {
        return conf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScmPropsExactMatchRule that = (ScmPropsExactMatchRule) o;
        return Objects.equals(conf, that.conf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conf);
    }
}
