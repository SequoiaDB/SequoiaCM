package com.sequoiacm.contentserver.service.impl;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.contentserver.dao.IAuditDao;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.service.IAuditService;
import com.sequoiacm.metasource.MetaCursor;

@Service
public class AuditServiceImpl implements IAuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    @Autowired
    private IAuditDao auditDao;
    
    @Override
    public MetaCursor getList(BSONObject matcher) throws ScmServerException {
        try {
            return auditDao.query(matcher);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("getAuditList failed : matcher={}", matcher);
            throw e;
        }
    }

}
