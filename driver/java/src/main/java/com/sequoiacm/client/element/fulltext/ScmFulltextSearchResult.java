package com.sequoiacm.client.element.fulltext;

import java.util.List;

import com.sequoiacm.client.element.ScmFileBasicInfo;

/**
 * Fulltext search result.
 */
public class ScmFulltextSearchResult {
    private ScmFileBasicInfo fileBasicInfo;
    private double score;
    private List<String> highlightTexts;

    public ScmFulltextSearchResult() {

    }

    /**
     * Get the file basic information.
     * @return file basic information.
     */
    public ScmFileBasicInfo getFileBasicInfo() {
        return fileBasicInfo;
    }

    /**
     * Get the score.
     * @return score.
     */
    public double getScore() {
        return score;
    }

    /**
     * Get the highlighted texts. 
     * @return  highlighted texts.
     */
    public List<String> getHighlightTexts() {
        return highlightTexts;
    }

    public void setFileBasicInfo(ScmFileBasicInfo fileBasicInfo) {
        this.fileBasicInfo = fileBasicInfo;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setHighlightTexts(List<String> highlightTexts) {
        this.highlightTexts = highlightTexts;
    }

    @Override
    public String toString() {
        return "ScmFulltextSearchRes [fileBasicInfo=" + fileBasicInfo + ", score=" + score
                + ", highLightTexts=" + highlightTexts + "]";
    }

}
