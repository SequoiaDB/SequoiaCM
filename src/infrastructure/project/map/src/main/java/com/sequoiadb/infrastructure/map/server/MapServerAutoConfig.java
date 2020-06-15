package com.sequoiadb.infrastructure.map.server;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.sequoiacm.infrastructure.metasource.EnableSdbDataSource;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

@Configuration
@EnableSdbDataSource
@ComponentScan(value = "com.sequoiadb.infrastructure.map.server", excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        MapServerWithoutDataSourceAutoConfig.class }))
public class MapServerAutoConfig {
    @Autowired
    private MapServer mapServer;

    @PostConstruct
    public void init() throws ScmMapServerException {
        mapServer.init();
    }

    public MapServerAutoConfig(MapServer mapServer) throws ScmMapServerException {
        mapServer.init();
    }
}
