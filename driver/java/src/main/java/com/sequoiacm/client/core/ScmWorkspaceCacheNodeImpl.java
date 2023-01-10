package com.sequoiacm.client.core;

import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import org.bson.BSONObject;

import java.io.InputStream;
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
    public void updateDataLocation(List<ScmDataLocation> dataLocations) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.updateDataLocation(dataLocations);
    }

    @Override
    public void updateDataLocation(List<ScmDataLocation> dataLocations, boolean mergeTo) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.updateDataLocation(dataLocations, mergeTo);
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName, String preferredRegion, String preferredZone)
            throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.applyTransition(transitionName, preferredRegion, preferredZone);
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName) throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.applyTransition(transitionName);
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName, ScmLifeCycleTransition transition,
            String preferredRegion, String preferredZone) throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.applyTransition(transitionName, transition, preferredRegion, preferredZone);
    }

    @Override
    public ScmTransitionSchedule applyTransition(String transitionName, ScmLifeCycleTransition transition)
            throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.applyTransition(transitionName, transition);
    }

    @Override
    public void setTransitionConfig(String xmlPath) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.setTransitionConfig(xmlPath);
    }

    @Override
    public void setTransitionConfig(InputStream xmlInputStream) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.setTransitionConfig(xmlInputStream);
    }

    @Override
    public void removeTransition(String transitionName) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.removeTransition(transitionName);
    }

    @Override
    public ScmTransitionSchedule updateTransition(String transitionName, ScmLifeCycleTransition transition)
            throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.updateTransition(transitionName, transition);
    }

    @Override
    public ScmTransitionSchedule getTransition(String transitionName) throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.getTransition(transitionName);
    }

    @Override
    public List<ScmTransitionSchedule> listTransition() throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.listTransition();
    }

    @Override
    public ScmTransitionSchedule updateTransition(String transitionName, ScmLifeCycleTransition transition,
            String preferredRegion, String preferredZone) throws ScmException {
        checkScmWorkspace();
        return scmWorkspace.updateTransition(transitionName, transition, preferredRegion, preferredZone);
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
    public void disableDirectory() throws ScmException {
        checkScmWorkspace();
        scmWorkspace.disableDirectory();
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

    @Override
    public void updatePreferred(String preferred) throws ScmException {
        checkScmWorkspace();
        scmWorkspace.updatePreferred(preferred);
    }

    @Override
    public ScmSiteCacheStrategy getSiteCacheStrategy() {
        checkScmWorkspace();
        return scmWorkspace.getSiteCacheStrategy();
    }

    @Override
    public void updateSiteCacheStrategy(ScmSiteCacheStrategy scmSiteCacheStrategy)
            throws ScmException {
        checkScmWorkspace();
        scmWorkspace.updateSiteCacheStrategy(scmSiteCacheStrategy);
    }

    @Override
    public String getPreferred() {
        checkScmWorkspace();
        return scmWorkspace.getPreferred();
    }
}
