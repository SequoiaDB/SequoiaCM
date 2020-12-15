package com.sequoiacm.tools.common;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.element.ScmWorkspaceInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

public class ScmMetaMgr {
    private Sequoiadb db;
    private DBCollection contentServerCL;
    private DBCollection workspaceCL;
    private DBCollection siteCL;
    private final Logger logger = LoggerFactory.getLogger(ScmMetaMgr.class);

    public ScmMetaMgr(Sequoiadb db) throws ScmToolsException {
        this.db = db;
        initCL();
    }

    public ScmMetaMgr(String sdb, String user, String pwd) throws ScmToolsException {
        db = SdbHelper.connectUrls(sdb, user, pwd);
        initCL();
    }

    private void initCL() throws ScmToolsException {
        CollectionSpace sysCS = SdbHelper.getCS(db, SdbHelper.CS_SYS);
        if (sysCS == null) {
            SdbHelper.closeCursorAndDb(db);
            logger.error("Can't find " + SdbHelper.CS_SYS + " on root site sdb");
            throw new ScmToolsException("Can't find " + SdbHelper.CS_SYS + " on root site sdb",
                    ScmExitCode.SCM_META_CS_MISSING);
        }
        try {
            contentServerCL = getCLWhithCheck(sysCS, SdbHelper.CL_CONTENTSERVER);
            siteCL = getCLWhithCheck(sysCS, SdbHelper.CL_SITE);
            workspaceCL = getCLWhithCheck(sysCS, SdbHelper.CL_WORKSPACE);
        }
        catch (ScmToolsException e) {
            SdbHelper.closeCursorAndDb(db);
            throw e;
        }
    }

    public int getContenserverIdByName(String name) throws ScmToolsException {
        DBCursor cursor = null;
        try {
            cursor = contentServerCL.query("{name:'" + name + "'}", null, null, null);
            if (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                Integer id = (Integer) obj.get("id");
                if (id == null) {
                    logger.error(name + " contentserver without id:" + obj);
                    throw new ScmToolsException(name + " contentserver without id",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                if (cursor.hasNext()) {
                    logger.error(name + " contentserver have more than one of record");
                    throw new ScmToolsException(
                            name + " contentserver have more than one of record",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                return id;
            }
            else {
                logger.error("Can't find contentserver id of " + name + " contentserver");
                throw new ScmToolsException(
                        "Can't find contentserver id of " + name + " contentserver",
                        ScmExitCode.SCM_SERVER_NOT_EXIST);
            }
        }
        catch (BaseException e) {
            logger.error("Failed to find contentserver id by name:" + name, e);
            throw new ScmToolsException("Failed to find contentserver id by name:" + name
                    + ",errorMsg:" + processSdbErrorMsg(e.getMessage()),
                    ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getContenserverNameById(int id) throws ScmToolsException {
        DBCursor cursor = null;
        try {
            cursor = contentServerCL.query("{id:" + id + "}", null, null, null);
            if (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                String name = (String) obj.get("name");
                if (name == null) {
                    logger.error(id + " contentserver without name:" + obj);
                    throw new ScmToolsException(id + " contentserver without name",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                if (cursor.hasNext()) {
                    logger.error(id + " contentserver have more than one of record");
                    throw new ScmToolsException(id + " contentserver have more than one of record",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                return name;
            }
            else {
                logger.error("Can't find contentserver name of " + id + " contentserver");
                throw new ScmToolsException(
                        "Can't find contentserver name of " + id + " contentserver",
                        ScmExitCode.SCM_SERVER_NOT_EXIST);
            }
        }
        catch (BaseException e) {
            logger.error("Failed to find contentserver by id:" + id, e);
            throw new ScmToolsException("Failed to find contentserver by id:" + id + ",errorMsg:"
                    + processSdbErrorMsg(e.getMessage()), ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int getSiteIdByName(String name) throws ScmToolsException {
        ScmSiteInfo info = getSiteInfoByName(name);
        if (info == null) {
            logger.error("Can't find site id of " + name + " site");
            throw new ScmToolsException("Can't find site id of " + name + " site",
                    ScmExitCode.SCM_SITE_NOT_EXIST);
        }
        else {
            return info.getId();
        }
    }

    public String getServerUrlById(int id) throws ScmToolsException {
        DBCursor cursor = null;
        try {
            cursor = contentServerCL.query("{id:" + id + "}", null, null, null);
            if (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                String hostName = (String) obj.get("host_name");
                if (hostName == null) {
                    logger.error(id + " contentserver without host name:" + obj);
                    throw new ScmToolsException(id + " contentserver without host name",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                Integer port = (Integer) obj.get("port");
                if (port == null) {
                    logger.error(id + " contentserver without port:" + obj);
                    throw new ScmToolsException(id + " contentserver without port",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                if (cursor.hasNext()) {
                    logger.error(id + " contentserver have more than one of record");
                    throw new ScmToolsException(id + " contentserver have more than one of record",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                return hostName + ":" + port;
            }
            else {
                logger.error("Can't find contentserver url of " + id + " contentserver");
                throw new ScmToolsException(
                        "Can't find contentserver url of " + id + " contentserver",
                        ScmExitCode.SCM_SERVER_NOT_EXIST);
            }
        }
        catch (BaseException e) {
            logger.error("Failed to find contentserver url by id:" + id, e);
            throw new ScmToolsException("Failed to find contentserver url by id:" + id
                    + ",errorMsg:" + processSdbErrorMsg(e.getMessage()),
                    ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getSiteNameById(int id) throws ScmToolsException {
        ScmSiteInfo site = getSiteInfoById(id);
        if (site == null) {
            logger.error("Can't find site name of " + id + " site");
            throw new ScmToolsException("Can't find site name of " + id + " site",
                    ScmExitCode.SCM_SITE_NOT_EXIST);
        }
        return site.getName();
    }

    public void close() {
        SdbHelper.closeCursorAndDb(db);
    }

    public int getSiteIdByContentsrverId(int id) throws ScmToolsException {
        DBCursor cursor = null;
        try {
            cursor = contentServerCL.query("{id:" + id + "}", null, null, null);
            if (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                Integer siteID = (Integer) obj.get("site_id");
                if (siteID == null) {
                    logger.error(id + " contentserver without site_id:" + obj);
                    throw new ScmToolsException(id + " contentserver without site_id",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                if (cursor.hasNext()) {
                    logger.error(id + " contentserver have more than one of record");
                    throw new ScmToolsException(id + " contentserver have more than one of record",
                            ScmExitCode.SCM_META_RECORD_ERROR);
                }
                return siteID;
            }
            else {
                logger.error("Can't find contentserver siteId of " + id + " contentserver");
                throw new ScmToolsException(
                        "Can't find contentserver siteId of " + id + " contentserver",
                        ScmExitCode.SCM_SERVER_NOT_EXIST);
            }
        }
        catch (BaseException e) {
            logger.error("Failed to find contentserver by id:" + id, e);
            throw new ScmToolsException("Failed to find contentserver by id:" + id + ",errorMsg:"
                    + processSdbErrorMsg(e.getMessage()), ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public ScmSiteInfo getSiteInfoById(int id) throws ScmToolsException {
        DBCursor cursor = null;
        try {
            cursor = siteCL.query("{id:" + id + "}", null, null, null);
            if (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                return new ScmSiteInfo(obj);
            }
            else {
                return null;
            }
        }
        catch (BaseException e) {
            logger.error("Failed to find site  by id:" + id, e);
            throw new ScmToolsException("Failed to find site  by id:" + id + ",errorMsg:"
                    + processSdbErrorMsg(e.getMessage()), ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public ScmSiteInfo getSiteInfoByName(String name) throws ScmToolsException {
        DBCursor cursor = null;
        try {
            cursor = siteCL.query("{name:'" + name + "'}", null, null, null);
            if (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                return new ScmSiteInfo(obj);
            }
            else {
                return null;
            }
        }
        catch (BaseException e) {
            logger.error("Failed to find site by name:" + name, e);
            throw new ScmToolsException("Failed to find site  by name:" + name + ",errorMsg:"
                    + processSdbErrorMsg(e.getMessage()), ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public ScmSiteInfo getMainSite() throws ScmToolsException {
        List<ScmSiteInfo> list = getSiteList();
        for (ScmSiteInfo info : list) {
            if (info.isRootSite()) {
                return info;
            }
        }
        return null;
    }

    public ScmSiteInfo getMainSiteChecked() throws ScmToolsException {
        ScmSiteInfo mainSite = getMainSite();
        if (mainSite == null) {
            throw new ScmToolsException("root site not exists", ScmExitCode.SCM_SITE_NOT_EXIST);
        }
        return mainSite;
    }

    public List<ScmSiteInfo> getSiteList() throws ScmToolsException {
        List<ScmSiteInfo> list = new ArrayList<>();
        DBCursor cursor = null;
        try {
            cursor = siteCL.query();
            while (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                ScmSiteInfo info = new ScmSiteInfo(obj);
                list.add(info);
            }
            return list;
        }
        catch (BaseException e) {
            logger.error("Query on site collection occur error", e);
            throw new ScmToolsException("Query on site collection occur error, errorMsg:"
                    + processSdbErrorMsg(e.getMessage()), ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int getWorkspaceIdByName(String name) throws ScmToolsException {
        ScmWorkspaceInfo info = getWorkspaceInfoByName(name);
        if (info != null) {
            return info.getId();
        }
        logger.error("Can't find worksapce by name:" + name);
        throw new ScmToolsException("Can't find worksapce by name:" + name,
                ScmExitCode.SCM_WORKSPACE_NOT_EXIST);
    }

    public ScmWorkspaceInfo getWorkspaceInfoByName(String name) throws ScmToolsException {
        DBCursor cursor = null;
        try {
            cursor = workspaceCL.query("{name:'" + name + "'}", null, null, null);
            if (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                return new ScmWorkspaceInfo(obj);
            }
            else {
                return null;
            }
        }
        catch (BaseException e) {
            logger.error("Failed to find worksapce  by name:" + name, e);
            throw new ScmToolsException("Failed to find worksapce  by name:" + name + ",errorMsg:"
                    + processSdbErrorMsg(e.getMessage()), ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public String getUser() throws ScmToolsException {
        CollectionSpace cs = SdbHelper.getCSWithCheck(db, SdbHelper.CS_SYS);
        DBCollection cl = SdbHelper.getCLWithCheck(cs, SdbHelper.CL_USER);
        BSONObject matcher = (BSONObject) JSON.parse("{user:{$exists:1}}");
        BSONObject obj = SdbHelper.queryOne(cl, matcher, null, null);
        return (String) obj.get("user");
    }

    public List<ScmWorkspaceInfo> listWorkspace() throws ScmToolsException {
        List<ScmWorkspaceInfo> list = new ArrayList<>();
        DBCursor cursor = null;
        try {
            cursor = workspaceCL.query();
            while (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                try {
                    ScmWorkspaceInfo info = new ScmWorkspaceInfo(obj);
                    list.add(info);
                }
                catch (ScmToolsException e) {
                    logger.warn("failed to parse workspace record:" + obj, e);
                }
            }
            return list;
        }
        catch (BaseException e) {
            logger.error("Query on workspace collection occur error", e);
            throw new ScmToolsException("Query on workspace collection occur error, errorMsg:"
                    + processSdbErrorMsg(e.getMessage()), ScmExitCode.SDB_QUERY_ERROR);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String processSdbErrorMsg(String msg) {
        String notNeed = "\n Exception Detail:";
        String notNeed2 = "\r\n Exception Detail:";
        if (msg.endsWith(notNeed)) {
            return msg.substring(0, msg.length() - notNeed.length());
        }
        else if (msg.endsWith(notNeed2)) {
            return msg.substring(0, msg.length() - notNeed2.length());
        }
        else {
            return msg;
        }
    }

    private DBCollection getCLWhithCheck(CollectionSpace cs, String clName)
            throws ScmToolsException {
        DBCollection cl = SdbHelper.getCL(cs, clName);
        if (cl == null) {
            logger.error("Can't find " + clName + " on root site sdb");
            throw new ScmToolsException("Can't find " + clName + " on root site sdb",
                    ScmExitCode.SCM_META_CL_MISSING);
        }
        return cl;
    }

}
