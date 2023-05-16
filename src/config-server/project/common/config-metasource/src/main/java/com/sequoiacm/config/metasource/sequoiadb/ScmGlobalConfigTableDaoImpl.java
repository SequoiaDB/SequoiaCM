package com.sequoiacm.config.metasource.sequoiadb;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.ScmGlobalConfigTableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.HashMap;
import java.util.Map;

public class ScmGlobalConfigTableDaoImpl implements ScmGlobalConfigTableDao {
    public static final String CS_NAME = "SCMSYSTEM";
    public static final String CL_NAME = "GLOBAL_CONFIG";
    private SequoiadbMetasource sdbMetaSource;

    public ScmGlobalConfigTableDaoImpl(SequoiadbMetasource sdbMetaSource)
            throws MetasourceException {
        this.sdbMetaSource = sdbMetaSource;
    }

    @Override
    public void setGlobalConfig(String configName, String configValue) throws MetasourceException {
        BasicBSONObject updater = new BasicBSONObject();
        updater.put(FieldName.GlobalConfig.FIELD_CONFIG_NAME, configName);
        updater.put(FieldName.GlobalConfig.FIELD_CONFIG_VALUE, configValue);
        sdbMetaSource.getCollection(CS_NAME, CL_NAME).upsert(
                new BasicBSONObject(FieldName.GlobalConfig.FIELD_CONFIG_NAME, configName),
                new BasicBSONObject("$set", updater));
    }

    @Override
    public String getGlobalConfig(String configName) throws MetasourceException {
        BSONObject ret = sdbMetaSource.getCollection(CS_NAME, CL_NAME).queryOne(
                new BasicBSONObject(FieldName.GlobalConfig.FIELD_CONFIG_NAME, configName), null,
                null);
        if (ret == null) {
            return null;
        }
        return (String) ret.get(FieldName.GlobalConfig.FIELD_CONFIG_VALUE);
    }

    @Override
    public Map<String, String> getAllGlobalConfig() throws MetasourceException {
        Map<String, String> ret = new HashMap<>();
        MetaCursor cursor = sdbMetaSource.getCollection(CS_NAME, CL_NAME).query(null, null, null);
        try {
            while (cursor.hasNext()) {
                BSONObject record = cursor.getNext();
                ret.put((String) record.get(FieldName.GlobalConfig.FIELD_CONFIG_NAME),
                        (String) record.get(FieldName.GlobalConfig.FIELD_CONFIG_VALUE));
            }
            return ret;
        }
        finally {
            cursor.close();
        }
    }
}
