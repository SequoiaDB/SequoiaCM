package com.sequoiacm.schedule.dao.sequoiadb;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.dao.FileServerDao;
import com.sequoiacm.schedule.entity.ConfigEntityTranslator;
import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

@Repository("FileServerDao")
public class SdbFileServerDao implements FileServerDao {
    private static final Logger logger = LoggerFactory.getLogger(SdbFileServerDao.class);
    private SdbDataSourceWrapper datasource;
    private String csName = "SCMSYSTEM";
    private String clName = "CONTENTSERVER";

    @Autowired
    public SdbFileServerDao(SdbDataSourceWrapper datasource) {
        this.datasource = datasource;
    }

    @Override
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher);
    }

    @Override
    public FileServerEntity queryOne(String nodeName) throws Exception {
        BSONObject matcher = new BasicBSONObject(FieldName.FileServer.FIELD_NAME, nodeName);
        Sequoiadb sdb = null;
        try {
            sdb = datasource.getConnection();
            CollectionSpace cs = sdb.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            BSONObject result = cl.queryOne(matcher, null, null, null, 0);
            if (null == result) {
                return null;
            }
            return ConfigEntityTranslator.FileServer.fromBSONObject(result);
        }
        catch (Exception e) {
            logger.error("query contentserver failed[cs={},cl={}]:matcher={}", csName, clName,
                    matcher);
            throw e;
        }
        finally {
            datasource.releaseConnection(sdb);
        }
    }
}