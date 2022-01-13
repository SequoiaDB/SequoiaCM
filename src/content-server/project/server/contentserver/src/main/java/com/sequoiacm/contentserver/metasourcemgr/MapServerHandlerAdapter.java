package com.sequoiacm.contentserver.metasourcemgr;

import com.sequoiacm.metasource.sequoiadb.IMetaSourceHandler;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.infrastructure.map.server.MapDataSourceHandler;

public class MapServerHandlerAdapter implements IMetaSourceHandler {
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
