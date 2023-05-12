package com.sequoiacm.cloud.authentication.dao;

import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructrue.security.core.ITransaction;

@Repository("IPrivVersionDao")
public class SequoiadbPrivVersionDao implements IPrivVersionDao {
    private static final String CS_SCMSYSTEM = "SCMSYSTEM";
    private static final String CL_PRIV_VERSION = "PRIV_VERSION";

    private static final String FIELD_VERSION = "version";

    private final SequoiadbDatasource datasource;
    private final SequoiadbTemplate template;

    @Autowired
    public SequoiadbPrivVersionDao(SequoiadbDatasource datasource) {
        this.datasource = datasource;
        this.template = new SequoiadbTemplate(datasource);
    }

    @Override
    public int getVersion() {
        BSONObject matcher = new BasicBSONObject();
        BSONObject obj = template.collection(CS_SCMSYSTEM, CL_PRIV_VERSION).findOne(matcher);
        if (obj == null) {
            return 0;
        }

        return (int) obj.get(FIELD_VERSION);
    }

    @Override
    public void incVersion() {
        incVersion(null);
    }

    @Override
    public void incVersion(ITransaction t) {
        BSONObject inc = new BasicBSONObject(FIELD_VERSION, 1);
        BSONObject updator = new BasicBSONObject("$inc", inc);
        template.collection(CS_SCMSYSTEM, CL_PRIV_VERSION).update(new BasicBSONObject(), updator,
                (SequoiadbTransaction) t);
    }

}
