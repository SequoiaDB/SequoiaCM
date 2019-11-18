package com.sequoiacm.contentserver.model;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;

public class ClientWorkspaceConf {
    private String wsName;
    private String description;
    private Integer wsId;
    private String createUser;
    private String updateUser;

    private ClientLocationOutline metaLocation;
    private List<ClientLocationOutline> datalocations = new ArrayList<>();

    public ClientWorkspaceConf(String wsName, BSONObject clientConf)
            throws ScmInvalidArgumentException {
        this.wsName = wsName;
        BSONObject metaLocation = (BSONObject) clientConf
                .get(FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        if (metaLocation == null) {
            throw new ScmInvalidArgumentException(
                    "missing " + FieldName.FIELD_CLWORKSPACE_META_LOCATION + ":" + clientConf);
        }
        this.metaLocation = new ClientLocationOutline(metaLocation);

        BasicBSONList clientDataLocations = (BasicBSONList) clientConf
                .get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION);
        if (clientDataLocations == null) {
            throw new ScmInvalidArgumentException(
                    "missing " + FieldName.FIELD_CLWORKSPACE_DATA_LOCATION + ":" + clientConf);
        }

        for (Object datalocation : clientDataLocations) {
            datalocations.add(new ClientLocationOutline((BSONObject) datalocation));
        }

        this.description = (String) clientConf.get(FieldName.FIELD_CLWORKSPACE_DESCRIPTION);
        if (this.description == null) {
            this.description = "";
        }

    }

    public BSONObject toCompleteBSON() {
        BasicBSONObject completeWsBSON = new BasicBSONObject();
        completeWsBSON.put(FieldName.FIELD_CLWORKSPACE_ID, wsId);
        completeWsBSON.put(FieldName.FIELD_CLWORKSPACE_NAME, wsName);
        completeWsBSON.put(FieldName.FIELD_CLWORKSPACE_CREATEUSER, createUser);
        completeWsBSON.put(FieldName.FIELD_CLWORKSPACE_UPDATEUSER, updateUser);
        completeWsBSON.put(FieldName.FIELD_CLWORKSPACE_DESCRIPTION, description);
        completeWsBSON.put(FieldName.FIELD_CLWORKSPACE_META_LOCATION,
                metaLocation.toCompleteBSON());
        BasicBSONList l = new BasicBSONList();
        for (ClientLocationOutline datalocation : datalocations) {
            l.add(datalocation.toCompleteBSON());
        }
        completeWsBSON.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, l);
        return completeWsBSON;
    }

    public void addWsId(int wsId) {
        this.wsId = wsId;
    }

    public void addCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public void addUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public ClientLocationOutline getMetaLocation() {
        return metaLocation;
    }

    public List<ClientLocationOutline> getDataLocations() {
        return datalocations;
    }

    public String getCreateUser() {
        return createUser;
    }

    public String getName() {
        return wsName;
    }

}
