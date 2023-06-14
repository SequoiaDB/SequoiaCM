package com.sequoiacm.infrastructure.metasource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import com.sequoiadb.base.UserConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.metasource.config.SequoiadbConfig;
import com.sequoiacm.infrastructure.metasource.template.DataSourceWrapper;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;

@Configuration
@ComponentScan("com.sequoiacm.infrastructure.metasource")
public class SdbDataSourceAutoConfig {
    private SequoiadbConfig configuration;

    @Autowired
    public SdbDataSourceAutoConfig(SequoiadbConfig configuration) {
        this.configuration = configuration;
        DataSourceWrapper.getInstance().init(createSdbDatasource());
    }

    @PreDestroy
    public void destroy() {
        DataSourceWrapper.getInstance().clear();
    }

    public SequoiadbDatasource createSdbDatasource() {
        ConfigOptions nwOpt = new ConfigOptions();
        nwOpt.setConnectTimeout(configuration.getConnectTimeout());
        nwOpt.setMaxAutoConnectRetryTime(configuration.getMaxAutoConnectRetryTime());
        nwOpt.setSocketKeepAlive(true);
        nwOpt.setSocketTimeout(configuration.getSocketTimeout());
        nwOpt.setUseNagle(configuration.getUseNagle());
        nwOpt.setUseSSL(configuration.getUseSSL());

        DatasourceOptions dsOpt = new DatasourceOptions();
        dsOpt.setMaxCount(configuration.getMaxConnectionNum());
        dsOpt.setDeltaIncCount(configuration.getDeltaIncCount());
        dsOpt.setMaxIdleCount(configuration.getMaxIdleNum());
        dsOpt.setKeepAliveTimeout(configuration.getKeepAliveTime());
        dsOpt.setCheckInterval(configuration.getRecheckCyclePeriod());
        dsOpt.setValidateConnection(configuration.getValidateConnection());
        List<String> preferedInstance = new ArrayList<>();
        preferedInstance.add("M");
        dsOpt.setPreferredInstance(preferedInstance);

        AuthInfo auth = ScmFilePasswordParser.parserFile(configuration.getPassword());
// 回退sdb驱动至349，不支持location：SEQUOIACM-1411
//        String location = configuration.getLocation() == null ? ""
//                : configuration.getLocation().trim();
//        return SequoiadbDatasource.builder().serverAddress(configuration.getUrls())
//                .userConfig(new UserConfig(configuration.getUsername(), auth.getPassword()))
//                .configOptions(nwOpt).datasourceOptions(dsOpt).location(location).build();
        return SequoiadbDatasource.builder().serverAddress(configuration.getUrls())
                .userConfig(new UserConfig(configuration.getUsername(), auth.getPassword()))
                .configOptions(nwOpt).datasourceOptions(dsOpt).build();
    }

}
