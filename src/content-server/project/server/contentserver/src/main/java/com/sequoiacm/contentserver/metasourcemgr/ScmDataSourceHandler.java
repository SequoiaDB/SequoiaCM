package com.sequoiacm.contentserver.metasourcemgr;

import com.sequoiacm.metasource.sequoiadb.IDataSourceHandler;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.infrastructure.map.server.MapDataSourceHandler;

public class ScmDataSourceHandler implements IDataSourceHandler {
    private MapDataSourceHandler mapHandler = new MapDataSourceHandler();

    @Override
    public void refresh(SequoiadbDatasource dataSource) {
        mapHandler.refresh(dataSource);
    }

    @Override
    public void clear() {
        mapHandler.clear();
    }
}
