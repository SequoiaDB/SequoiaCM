package com.sequoiacm.contentserver.config;

import com.sequoiacm.infrastructure.sdbversion.VersionRange;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@ConfigurationProperties("scm.tag")
public class ScmTagConfig {

    // 支持填写指定版本号，如：3.6.1
    // 支持填写版本范围，如：(3.6.1, 4.0.0]
    // 支持填写多个范围如：(3.6.1, 4.0.0],(5.0, 6.0)
    private String sdbRequiredVersion = "3.6.1";

    private List<VersionRange> versionRange;

    public String getSdbRequiredVersion() {
        return sdbRequiredVersion;
    }

    public void setSdbRequiredVersion(String sdbRequiredVersion) {
        this.sdbRequiredVersion = sdbRequiredVersion;
    }
    
    @PostConstruct
    public void init() {
        this.versionRange = VersionRange.parse(sdbRequiredVersion);
    }

    public List<VersionRange> getSdbVersionRange() {
        return versionRange;
    }
}
