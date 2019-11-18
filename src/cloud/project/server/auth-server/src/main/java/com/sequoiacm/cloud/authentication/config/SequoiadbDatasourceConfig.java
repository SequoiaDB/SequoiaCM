package com.sequoiacm.cloud.authentication.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.SequoiadbDatasource;
import com.sequoiadb.datasource.DatasourceOptions;

@Configuration
public class SequoiadbDatasourceConfig {
    @Autowired
    private SequoiadbConfig configuration;

    @Bean
    public SequoiadbDatasource sequoiadbDatasource() {

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
        dsOpt.setPreferedInstance(preferedInstance);

        AuthInfo auth = ScmFilePasswordParser.parserFile(configuration.getPassword());

        return new SequoiadbDatasource(configuration.getUrls(), configuration.getUsername(),
                auth.getPassword(), nwOpt, dsOpt);

    }
}
