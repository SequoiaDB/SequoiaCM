package com.sequoiacm.config.framework.metadata.metasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;

@Component
public class MetaDataConfMetaServiceSdbImpl implements MetaDataConfMetaService {
    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public TableDao getAttributeTableDao(String wsName, Transaction t) throws MetasourceException {
        return sdbMetasource.getCollection(t,
                wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                MetaSourceDefine.SequoiadbTableName.CL_ATTRIBUTE);
    }

    @Override
    public TableDao getClassTableDao(String wsName, Transaction t) throws MetasourceException {
        return sdbMetasource.getCollection(t,
                wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                MetaSourceDefine.SequoiadbTableName.CL_CLASS);
    }

    @Override
    public TableDao getAttributeClassRelTableDao(String wsName, Transaction t)
            throws MetasourceException {
        return sdbMetasource.getCollection(t,
                wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                MetaSourceDefine.SequoiadbTableName.CL_CLASS_ATTR_REL);
    }

}
