package com.sequoiacm.contentserver.dao.impl;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.sequoiacm.contentserver.dao.IAuditDao;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.metasource.MetaCursor;

@Repository
public class AuditDaoImpl implements IAuditDao {
    private static final Logger logger = LoggerFactory.getLogger(AuditDaoImpl.class);
    
    @Override
    public MetaCursor query(BSONObject matcher) throws ScmServerException {
        try {
            MetaCursor cursor = ScmContentServer.getInstance().getMetaService()
                    .getAuditList(matcher);

            return cursor;
        }
        catch (ScmServerException e) {
            logger.error("query failed:matcher={}", matcher);
            throw e;
        }
    }

}
