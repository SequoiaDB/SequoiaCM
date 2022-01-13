package com.sequoiadb.infrastructure.map.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.metasource.template.DataSourceWrapper;
import com.sequoiadb.datasource.SequoiadbDatasource;

public class MapDataSourceHandler {
    private static final Logger logger = LoggerFactory.getLogger(MapDataSourceHandler.class);
    private static boolean enableDataSource = true;

    public static void setEnableDataSource(boolean enableDataSource) {
        MapDataSourceHandler.enableDataSource = enableDataSource;
    }

    public void refresh(SequoiadbDatasource dataSource) {
        if (enableDataSource) {
            // 这个分支表示当前 Map Server 配置为使用私有的 datasource，而非外部宿主服务的 datasource，所以不允许外部传入 datasource 实例
            throw new UnsupportedOperationException("refresh datasource");
        }
        DataSourceWrapper.getInstance().init(dataSource);
    }

    public void clear() {
        if (enableDataSource) {
            // clear 方法是外部宿主服务通知 Map Server，其通过 refresh 接口传入的 datasource 需要关闭
            // 这个分支表示当前 Map Server 配置为使用私有的 datasource，而非外部宿主服务的 datasource，所以不允许 clear
            throw new UnsupportedOperationException("clear datasource");
        }
        DataSourceWrapper.getInstance().clear();
    };

}
