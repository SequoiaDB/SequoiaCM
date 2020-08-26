package com.sequoiacm.infrastructure.config.core.msg.workspace;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;

public class WorkspaceUpdator implements ConfigUpdator {
    private String wsName;
    private Integer removeDataLocationId;
    private BSONObject addDataLocation;
    private String newDesc;
    private BSONObject externalData;
    private BSONObject oldWsRecord;

    public WorkspaceUpdator(String wsName) {
        this.wsName = wsName;
    }

    public WorkspaceUpdator(String wsName, BSONObject oldWsRecord) {
        this.wsName = wsName;
        this.oldWsRecord = oldWsRecord;
    }

    public void setExternalData(BSONObject externalData) {
        this.externalData = externalData;
    }

    public BSONObject getExternalData() {
        return externalData;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_WORKSPACENAME, wsName);
        BasicBSONObject updator = new BasicBSONObject();
        if (newDesc != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_DESCRIPTION, newDesc);
        }
        if (addDataLocation != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_ADD_DATALOCATION, addDataLocation);
        }

        if (removeDataLocationId != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_REMOVE_DATALOCATION, removeDataLocationId);
        }
        if (externalData != null) {
            updator.put(ScmRestArgDefine.WORKSPACE_CONF_EXTERNAL_DATA, externalData);
        }
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_UPDATOR, updator);
        obj.put(ScmRestArgDefine.WORKSPACE_CONF_OLD_WS, oldWsRecord);
        return obj;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public Integer getRemoveDataLocationId() {
        return removeDataLocationId;
    }

    public void setRemoveDataLocationId(Integer removeDataLocationId) {
        this.removeDataLocationId = removeDataLocationId;
    }

    public BSONObject getAddDataLocation() {
        return addDataLocation;
    }

    public void setAddDataLocation(BSONObject addDataLocation) {
        this.addDataLocation = addDataLocation;
    }

    public String getNewDesc() {
        return newDesc;
    }

    public void setNewDesc(String newDesc) {
        this.newDesc = newDesc;
    }

    public BSONObject getOldWsRecord() {
        return oldWsRecord;
    }

    public void setOldWsRecord(BSONObject oldWsRecord) {
        this.oldWsRecord = oldWsRecord;
    }

}
