package com.sequoiacm.config.framework.config.site.metasource;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;

public interface SiteMetaService {
    TableDao getSysSiteTable(Transaction transaction);

    TableDao getSysSiteTable();
}
