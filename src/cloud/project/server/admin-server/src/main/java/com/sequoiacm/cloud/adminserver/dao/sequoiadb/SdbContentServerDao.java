package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.dao.ContentServerDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.BsonTranslator;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;

@Repository
public class SdbContentServerDao implements ContentServerDao {

    @Autowired
    private SequoiadbMetaSource metasource;
    
    @Override
    public List<ContentServerInfo> queryAll() throws StatisticsException {
        MetaAccessor contentServerAccessor = metasource.getContentServerAccessor();

        MetaCursor cursor = null;
        try {
            cursor = contentServerAccessor.query(null, null, null);
            List<ContentServerInfo> serverList = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject bo = cursor.getNext();
                ContentServerInfo contentServer = BsonTranslator.ContentServer.fromBSONObject(bo);
                serverList.add(contentServer);
            }
            return serverList;
        }
        finally {
            SequoiadbHelper.closeCursor(cursor);
        }
    }

}
