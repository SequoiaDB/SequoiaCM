package com.sequoiacm.fulltext.server.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.infrastructure.fulltext.common.FultextRestCommonDefine;

public class FulltextSearchRes {
    @JsonProperty(FultextRestCommonDefine.FulltextSearchRes.KEY_FILE_BASIC_INFO)
    private ScmFileBasicInfo fileBasicInfo;
    @JsonProperty(FultextRestCommonDefine.FulltextSearchRes.KEY_SCORE)
    private float score;
    @JsonProperty(FultextRestCommonDefine.FulltextSearchRes.KEY_HIGHLIGHT)
    private List<String> highLight;

    public FulltextSearchRes() {
    }

    public FulltextSearchRes(ScmFileInfo scmfile, float score, List<String> highLight) {
        fileBasicInfo = new ScmFileBasicInfo();
        fileBasicInfo.setId(scmfile.getId());
        fileBasicInfo.setName(scmfile.getName());
        fileBasicInfo.setCreateUser(scmfile.getCreateUser());
        fileBasicInfo.setCreateTime(scmfile.getCreateTime());
        fileBasicInfo.setMimeType(scmfile.getMimeType().getType());
        fileBasicInfo.setMajorVersion(scmfile.getMajorVersion());
        fileBasicInfo.setMinorVersion(scmfile.getMinorVersion());
        this.highLight = highLight;
        this.score = score;
    }

    public ScmFileBasicInfo getFileBasicInfo() {
        return fileBasicInfo;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public List<String> getHighLight() {
        return highLight;
    }

    public void setHighLight(List<String> highLight) {
        this.highLight = highLight;
    }

}
