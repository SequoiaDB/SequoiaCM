package com.sequoiacm.fulltext.es.client.base;

import java.util.List;

public class EsSearchRes {
    
    
    private String fileId;
    private String fileVersion;
    private List<String> highlight;
    private float score;
    private String docId;

    public EsSearchRes(String docId, String fileId, String fileVersion, float score,
            List<String> highlight) {
        this.fileId = fileId;
        this.fileVersion = fileVersion;
        this.highlight = highlight;
        this.score = score;
        this.docId = docId;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(String fileVersion) {
        this.fileVersion = fileVersion;
    }

    public List<String> getHighlight() {
        return highlight;
    }

    public void setHighlight(List<String> highlight) {
        this.highlight = highlight;
    }

    @Override
    public String toString() {
        return "FulltextIdxQueryResDoc [fileId=" + fileId + ", fileVersion=" + fileVersion
                + ", highlight=" + highlight + "]";
    }

}
