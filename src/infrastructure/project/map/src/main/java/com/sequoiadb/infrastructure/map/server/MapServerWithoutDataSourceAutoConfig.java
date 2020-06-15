package com.sequoiadb.infrastructure.map.server;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.sequoiadb.infrastructure.map.ScmMapServerException;

@Configuration
@ComponentScan(value = "com.sequoiadb.infrastructure.map.server", excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        MapServerAutoConfig.class }))
public class MapServerWithoutDataSourceAutoConfig {
    @Autowired
    private MapServer mapServer;

    @PostConstruct
    public void init() throws ScmMapServerException {
        mapServer.init();
    }

    public MapServerWithoutDataSourceAutoConfig() throws ScmMapServerException {
        MapDataSourceHandler.setEnableDataSource(false);
    }

}
