package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;

public class ClientWorkspaceUpdator {
    private String description;
    private String removeDataLocation;
    private ClientLocationOutline addDataLocation;
    private String preferred;

    public ClientWorkspaceUpdator() {
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred;
    }

    public String getPreferred() {
        return preferred;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRemoveDataLocation() {
        return removeDataLocation;
    }

    public void setRemoveDataLocation(String removeDataLocation) {
        this.removeDataLocation = removeDataLocation;
    }

    public ClientLocationOutline getAddDataLocation() {
        return addDataLocation;
    }

    public void setAddDataLocation(ClientLocationOutline addDataLocation) {
        this.addDataLocation = addDataLocation;
    }

    @Override
    public String toString() {
        return "ClientWorkspaceUpdator{" + "description='" + description + '\''
                + ", removeDataLocation='" + removeDataLocation + '\'' + ", addDataLocation="
                + addDataLocation + ", preferred='" + preferred + '\'' + '}';
    }

    public static ClientWorkspaceUpdator fromBSONObject(BSONObject obj)
            throws ScmInvalidArgumentException {
        BSONObject objCopy = new BasicBSONObject(obj.toMap());
        ClientWorkspaceUpdator updator = new ClientWorkspaceUpdator();
        String desc = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_DESCRIPTION);
        updator.setDescription(desc);
        String removeDataLocation = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_REMOVE_DATA_LOCATION);
        updator.setRemoveDataLocation(removeDataLocation);
        BSONObject addDataLocation = (BSONObject) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_ADD_DATA_LOCATION);
        if (addDataLocation != null) {
            updator.setAddDataLocation(new ClientLocationOutline(addDataLocation));
        }
        String preferred = (String) objCopy
                .removeField(CommonDefine.RestArg.WORKSPACE_UPDATOR_PREFERRED);
        updator.setPreferred(preferred);
        if (!objCopy.isEmpty()) {
            throw new ScmInvalidArgumentException(
                    "failed to update workspace, updator contain invalid key:" + objCopy.keySet());
        }
        return updator;
    }
}
