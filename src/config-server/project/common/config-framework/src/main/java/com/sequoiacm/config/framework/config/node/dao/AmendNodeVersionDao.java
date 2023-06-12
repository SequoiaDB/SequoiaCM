package com.sequoiacm.config.framework.config.node.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sequoiacm.config.framework.config.node.metasource.NodeMetaService;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
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
                    .getVerions(new VersionFilter(ScmBusinessTypeDefine.NODE));
            for (Version version : versionList) {
                configNameSet.add(version.getBusinessName());
            }

            tableDao = nodeMetaService.getContentServerTableDao();
            metaCursor = tableDao.query(null, null, null);
            while (metaCursor.hasNext()) {
                BSONObject tableRecord = metaCursor.getNext();
                String bussinessName = (String) tableRecord
                        .get(FieldName.FIELD_CLCONTENTSERVER_NAME);
                if (!configNameSet.contains(bussinessName)) {
                    try {
                        versionDao.createVersion(ScmBusinessTypeDefine.NODE, bussinessName);
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
