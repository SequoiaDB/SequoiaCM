package com.sequoiacm.metasource.sequoiadb;

import com.sequoiadb.datasource.SequoiadbDatasource;

public interface IDataSourceHandler {
    public void refresh(SequoiadbDatasource dataSource);

    public void clear();
}
