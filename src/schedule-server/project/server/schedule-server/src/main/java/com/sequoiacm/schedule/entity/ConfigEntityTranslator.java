package com.sequoiacm.schedule.entity;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.schedule.common.FieldName;

public class ConfigEntityTranslator {
    public static class Site {
        public static SiteEntity fromBSONObject(BSONObject obj) {
            SiteEntity site = new SiteEntity();
            site.setId((int) obj.get(FieldName.Site.FIELD_ID));
            site.setName((String) obj.get(FieldName.Site.FIELD_NAME));
            site.setRoot((boolean) obj.get(FieldName.Site.FIELD_ROOT_FLAG));
            site.setStageTag((String) obj.get(FieldName.Site.FIELD_STAGE_TAG));
            return site;
        }
    }

    public static class Workspace {
        public static WorkspaceEntity fromBSONObject(BSONObject obj) {
            WorkspaceEntity workspace = new WorkspaceEntity();
            workspace.setId((int) obj.get(FieldName.Workspace.FIELD_ID));
            workspace.setName((String) obj.get(FieldName.Workspace.FIELD_NAME));

            BasicBSONList siteList = (BasicBSONList) obj
                    .get(FieldName.Workspace.FIELD_DATA_LOCATION);

            for (int i = 0; i < siteList.size(); i++) {
                BSONObject siteObj = (BSONObject) siteList.get(i);
                workspace.addSite((int) siteObj.get(FieldName.Workspace.FIELD_SITE_ID));
            }

            return workspace;
        }
    }

    public static class FileServer {
        public static FileServerEntity fromBSONObject(BSONObject obj) {
            FileServerEntity server = new FileServerEntity();
            server.setId((int) obj.get(FieldName.FileServer.FIELD_ID));
            server.setName((String) obj.get(FieldName.FileServer.FIELD_NAME));
            server.setHostName((String) obj.get(FieldName.FileServer.FIELD_HOSTNAME));
            server.setPort((int) obj.get(FieldName.FileServer.FIELD_PORT));
            server.setSiteId((int) obj.get(FieldName.FileServer.FIELD_SITE_ID));
            return server;
        }
    }
}
