package com.sequoiacm.metasource.sequoiadb;

import com.sequoiadb.datasource.SequoiadbDatasource;

public interface IMetaSourceHandler {
    public void refresh(SequoiadbDatasource dataSource);

    public void clear();
}
