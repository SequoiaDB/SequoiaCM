package com.sequoiacm.config.framework.node.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.node.metasource.NodeMetaService;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;

@Component
public class AmendNodeVersionDao {

    @Autowired
    private NodeMetaService nodeMetaService;

    @Autowired
    private DefaultVersionDao versionDao;

    public void amend() throws ScmConfigException {
        TableDao tableDao = null;
        MetaCursor metaCursor = null;
        try {
            Set<String> configNameSet = new HashSet<>();
            List<Version> versionList = versionDao
                    .getVerions(new DefaultVersionFilter(ScmConfigNameDefine.NODE));
            for (Version version : versionList) {
                configNameSet.add(version.getBussinessName());
            }

            tableDao = nodeMetaService.getContentServerTableDao();
            metaCursor = tableDao.query(null, null, null);
            while (metaCursor.hasNext()) {
                BSONObject tableRecord = metaCursor.getNext();
                String bussinessName = (String) tableRecord
                        .get(FieldName.FIELD_CLCONTENT_SERVER_NAME);
                if (!configNameSet.contains(bussinessName)) {
                    try {
                        versionDao.createVersion(ScmConfigNameDefine.NODE, bussinessName);
                    }
                    catch (MetasourceException e) {
                        if (e.getError() != ScmConfError.METASOURCE_RECORD_EXIST) {
                            throw e;
                        }
                    }
                }
            }
        }
        catch (ScmConfigException e) {
            if (e.getError() != ScmConfError.METASOURCE_TABLE_NOT_EXIST) {
                throw e;
            }
        }
        catch (Exception e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "amend content node versions failed", e);
        }
        finally {
            if (metaCursor != null) {
                metaCursor.close();
            }
        }

    }
}
