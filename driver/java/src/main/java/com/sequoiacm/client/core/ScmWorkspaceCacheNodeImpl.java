package com.sequoiacm.client.core;

import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import org.bson.BSONObject;

import java.util.Date;
import java.util.List;

public class ScmWorkspaceCacheNodeImpl extends ScmWorkspace {

    private String name;
    private ScmSession session;
    private ScmWorkspaceImpl scmWorkspace;

    public ScmWorkspaceCacheNodeImpl(String name, ScmSession session) {
        this.name = name;
        this.session = session;
    }

    private void checkScmWorkspace() {
        if (scmWorkspace == null) {
            try {
                BSONObject wsBSON = session.getDispatcher().getWorkspace(name);
                scmWorkspace = new ScmWorkspaceImpl(session, wsBSON);
            }
            catch (Exception e) {
                throw new RuntimeException(
                        "failed to get workspace through session, workspace name is " + name, e);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ScmMetaLocation getMetaLocation() {
        checkScmWorkspace();
        return scmWorkspace.getMetaLocation();
    }

    @Override
    public List<ScmDataLocation> getDataLocations() {
        checkScmWorkspace();
        return scmWorkspace.getDataLocations();
    }

    @Override
    public String getDescription() {
        checkScmWorkspace();
        return scmWorkspace.getDescription();
    }

    @Override
    public String getCreateUser() {
        checkScmWorkspace();
        return scmWorkspace.getCreateUser();
    }

    @Override
    public String getUpdateUser() {
        checkScmWorkspace();
        return scmWorkspace.getUpdateUser();
    }

    @Override
    public Date getUpdateTime() {
        checkScmWorkspace();
        return scmWorkspace.getUpdateTime();
    }

    @Override
    public Date getCreateTime() {
        checkScmWorkspace();
        return scmWorkspace.getCreateTime();
    }

    @Override
    public void updatedDescription(String newDescription) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.updatedDescription(newDescription);
    }

    @Override
    public void addDataLocation(ScmDataLocation dataLocation) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.addDataLocation(dataLocation);
    }

    @Override
    public void removeDataLocation(String siteName) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.removeDataLocation(siteName);
    }

    @Override
    public ScmShardingType getBatchShardingType() {
        checkScmWorkspace();
        return scmWorkspace.getBatchShardingType();
    }

    @Override
    public String getBatchIdTimeRegex() {
        checkScmWorkspace();
        return scmWorkspace.getBatchIdTimeRegex();
    }

    @Override
    public String getBatchIdTimePattern() {
        checkScmWorkspace();
        return scmWorkspace.getBatchIdTimePattern();
    }

    @Override
    public boolean isBatchFileNameUnique() {
        checkScmWorkspace();
        return scmWorkspace.isBatchFileNameUnique();
    }

    @Override
    public boolean isEnableDirectory() {
        checkScmWorkspace();
        return scmWorkspace.isEnableDirectory();
    }

    @Override
    ScmSession getSession() {
        return session;
    }

    @Override
    int getId() {
        checkScmWorkspace();
        return scmWorkspace.getId();
    }

    @Override
    void setId(int id) {
        checkScmWorkspace();
        scmWorkspace.setId(id);
    }

    @Override
    BSONObject getExtData() {
        checkScmWorkspace();
        return scmWorkspace.getExtData();
    }
}
