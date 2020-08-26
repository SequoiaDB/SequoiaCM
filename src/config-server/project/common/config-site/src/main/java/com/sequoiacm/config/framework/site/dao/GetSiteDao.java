package com.sequoiacm.config.framework.site.dao;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.site.metasource.SiteMetaService;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverterMgr;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

@Component
public class GetSiteDao {
    private static final Logger logger = LoggerFactory.getLogger(GetSiteDao.class);

    @Autowired
    private SiteMetaService siteMetaService;
    @Autowired
    private BsonConverterMgr bsonConverterMgr;

    public List<Config> get(ConfigFilter filter) throws ScmConfigException {
        TableDao siteTable = siteMetaService.getSysSiteTable(null);
        MetaCursor cursor = siteTable.query(filter.toBSONObject(), null, null);
        try {
            List<Config> ret = new ArrayList<>();
            while (cursor.hasNext()) {
                Config c = bsonConverterMgr.getMsgConverter(ScmConfigNameDefine.SITE)
                        .convertToConfig(cursor.getNext());
                ret.add(c);
            }
            return ret;
        }
        finally {
            cursor.close();
        }
    }

}
