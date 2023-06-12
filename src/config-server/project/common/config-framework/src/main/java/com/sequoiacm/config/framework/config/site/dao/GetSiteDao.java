package com.sequoiacm.config.framework.config.site.dao;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.config.site.metasource.SiteMetaService;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteFilter;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;


@Component
public class GetSiteDao {
    private static final Logger logger = LoggerFactory.getLogger(GetSiteDao.class);

    @Autowired
    private SiteMetaService siteMetaService;

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;

    public List<Config> get(SiteFilter filter) throws ScmConfigException {
        TableDao siteTable = siteMetaService.getSysSiteTable(null);
        BasicBSONObject cond = new BasicBSONObject();
        if (filter.getSiteName() != null) {
            cond.put(FieldName.FIELD_CLSITE_NAME, filter.getSiteName());
        }
        MetaCursor cursor = siteTable.query(cond, null,
                null);
        try {
            List<Config> ret = new ArrayList<>();
            while (cursor.hasNext()) {
                Config c = configEntityTranslator.fromConfigBSON(ScmBusinessTypeDefine.SITE,
                        cursor.getNext());
                ret.add(c);
            }
            return ret;
        }
        finally {
            cursor.close();
        }
    }

}
