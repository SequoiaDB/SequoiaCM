package com.sequoiacm.clean;

import com.sequoiacm.common.mapping.ScmMappingException;
import com.sequoiacm.common.mapping.ScmSiteObj;
import com.sequoiacm.common.mapping.ScmWorkspaceObj;
import com.sequoiacm.contentserver.site.ScmContentServerMapping;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class SiteInfoMgr {
    private List<String> holdingDataSiteInstances;
    private final int cleanSiteId;
    private ScmLocation cleanSiteDataLocation;
    private ConcurrentHashMap<String, ScmSiteObj> siteNameObjMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> siteIdNameMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, List<String>> siteInstance = new ConcurrentHashMap<>();
    private List<Integer> wsDataLocationSiteId = new ArrayList<>();

    public SiteInfoMgr(SequoiadbDatasource metaSdbDs, String wsName, String cleanSiteName, List<String> holdingDataSiteInstances) 
            throws ScmMappingException, InterruptedException, ScmServerException, ScmDatasourceException {
        this.holdingDataSiteInstances = holdingDataSiteInstances;
        Sequoiadb db = metaSdbDs.getConnection();
        DBCursor cursor = null;
        try {
            CollectionSpace sysCs = db.getCollectionSpace("SCMSYSTEM");
            DBCollection siteCl = sysCs.getCollection("SITE");
            cursor = siteCl.query();
            while (cursor.hasNext()) {
                BSONObject rec = cursor.getNext();
                ScmSiteObj site = new ScmSiteObj(rec);
                siteNameObjMap.put(site.getName(), site);
                siteIdNameMap.put(site.getId(), site.getName());
            }
            cursor.close();
            this.cleanSiteId = getSiteObjByName(cleanSiteName).getId();

            DBCollection contentServerCl = sysCs.getCollection("CONTENTSERVER");
            cursor = contentServerCl.query();
            while (cursor.hasNext()) {
                BSONObject rec = cursor.getNext();
                ScmContentServerMapping cs = new ScmContentServerMapping(rec);
                List<String> csList = siteInstance.get(cs.getSite_id());
                if (csList == null) {
                    csList = new ArrayList<>();
                    siteInstance.put(cs.getSite_id(), csList);
                }
                csList.add(cs.getHost_name() + ":" + cs.getPort());
            }

            DBCollection wsCl = sysCs.getCollection("WORKSPACE");
            BSONObject wsRecord = wsCl.queryOne(new BasicBSONObject("name", wsName), null, null,
                    null, 0);
            if (wsRecord == null) {
                throw new RuntimeException("workspace not exist:" + wsName);
            }
            ScmWorkspaceObj ws = new ScmWorkspaceObj(wsRecord);
            for (BSONObject data : ws.getDataLocation()) {
                int siteId = BsonUtils.getNumberChecked(data, "site_id").intValue();
                if (siteId == cleanSiteId) {
                    String dataType = getSiteObjByName(cleanSiteName).getDataType();
                    cleanSiteDataLocation = ScmDatasourceUtil.createDataLocation(dataType, data, cleanSiteName);
                }
                wsDataLocationSiteId.add(siteId);
            }
            if (cleanSiteDataLocation == null) {
                throw new RuntimeException(
                        "clean site not in ws:" + wsName + ", clean site " + cleanSiteId);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
            metaSdbDs.releaseConnection(db);
        }
    }

    public String getHoldingDataSiteInstanceUrl(int siteId) {
        if (holdingDataSiteInstances == null || holdingDataSiteInstances.isEmpty()) {
            holdingDataSiteInstances = siteInstance.get(siteId);
        }
        Random rd = new Random();
        return holdingDataSiteInstances.get(rd.nextInt(holdingDataSiteInstances.size()));
    }

    public ScmSiteObj getSiteObjByName(String siteName) {
        ScmSiteObj siteObj = siteNameObjMap.get(siteName);
        if (siteObj == null) {
            throw new RuntimeException("site not found:" + siteName);
        }
        return siteObj;
    }

    public String getSiteNameById(int siteId) {
        String siteName = siteIdNameMap.get(siteId);
        if (StringUtils.isEmpty(siteName)) {
            throw new RuntimeException("site not found:" + siteId);
        }
        return siteName;
    }

    public ScmLocation getCleanSiteDataLocation() {
        return cleanSiteDataLocation;
    }

    public List<Integer> getWsLocationSiteId() {
        return wsDataLocationSiteId;
    }
}
