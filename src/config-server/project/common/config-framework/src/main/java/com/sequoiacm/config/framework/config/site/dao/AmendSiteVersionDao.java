package com.sequoiacm.config.framework.config.site.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sequoiacm.config.framework.config.site.metasource.SiteMetaService;
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
public class AmendSiteVersionDao {
    @Autowired
    private SiteMetaService siteMetaService;

    @Autowired
    private DefaultVersionDao versionDao;

    public void amend() throws ScmConfigException {
        TableDao tableDao = null;
        MetaCursor metaCursor = null;
        try {
            Set<String> configNameSet = new HashSet<>();
            List<Version> versionList = versionDao
                    .getVerions(new VersionFilter(ScmBusinessTypeDefine.SITE));
            for (Version version : versionList) {
                configNameSet.add(version.getBusinessName());
            }

            tableDao = siteMetaService.getSysSiteTable();
            metaCursor = tableDao.query(null, null, null);
            while (metaCursor.hasNext()) {
                BSONObject tableRecord = metaCursor.getNext();
                String bussinessName = (String) tableRecord.get(FieldName.FIELD_CLSITE_NAME);
                if (!configNameSet.contains(bussinessName)) {
                    try {
                        versionDao.createVersion(ScmBusinessTypeDefine.SITE, bussinessName);
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
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR, "amend site versions failed",
                    e);
        }
        finally {
            if (metaCursor != null) {
                metaCursor.close();
            }
        }
    }

}
