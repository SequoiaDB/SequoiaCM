package com.sequoiacm.infrastructure.config.core.customizer;

public interface ConfigCustomizer {
    VersionHeartbeatOption heartbeatOption();
}

class DefaultConfigCustomizer implements ConfigCustomizer {

    public static final ConfigCustomizer DEFAULT = new DefaultConfigCustomizer();

    @Override
    public VersionHeartbeatOption heartbeatOption() {
        return VersionHeartbeatOption.DEFAULT;
    }
}
