package com.sequoiadb.infrastructure.map.server;

import com.sequoiacm.infrastructure.metasource.template.DataSourceWrapper;
import com.sequoiadb.datasource.SequoiadbDatasource;

public class MapDataSourceHandler {
    private static boolean enableDataSource = true;

    public static void setEnableDataSource(boolean enableDataSource) {
        MapDataSourceHandler.enableDataSource = enableDataSource;
    }

    public void refresh(SequoiadbDatasource dataSource) {
        if (enableDataSource) {
            throw new UnsupportedOperationException("refresh datasource");
        }
        DataSourceWrapper.getInstance().init(dataSource);
    }

    public void clear() {
        if (enableDataSource) {
            throw new UnsupportedOperationException("clear datasource");
        }
        DataSourceWrapper.getInstance().clear();
    };

}
