package com.sequoiacm.transfer;

import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.common.mapping.ScmSiteObj;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class SiteInfoMgr {
    private ConcurrentHashMap<String, Integer> sites = new ConcurrentHashMap<>();

    public SiteInfoMgr(Sequoiadb sdb) throws ScmMappingException {
        DBCursor cursor = null;
        try {
            CollectionSpace sysCs = sdb.getCollectionSpace("SCMSYSTEM");
            DBCollection siteCl = sysCs.getCollection("SITE");
            cursor = siteCl.query();
            while (cursor.hasNext()) {
                BSONObject rec = cursor.getNext();
                ScmSiteObj site = new ScmSiteObj(rec);
                sites.put(site.getName(), site.getId());
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    public Integer getSiteIdByName(String siteName) {
        Integer siteId = sites.get(siteName);
        if (siteId == null) {
            throw new RuntimeException("site not found:" + siteName);
        }
        return siteId;
    }
}
