package com.sequoiacm.client.element.fulltext;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;

/**
 * The option of fulltext search highlight.
 */
public class ScmFulltextHighlightOption {
    private String preTag = "<em>";
    private String postTag = "</em>";
    private int fragmentSize = 100;
    private int numOfFragments = 5;

    /**
     * Create a instance with default options.
     */
    public ScmFulltextHighlightOption() {
    }

    /**
     * Create a instance with specified options.
     * 
     * @param preTag
     *            use in conjunction with preTag to define the html tags to use
     *            for highlighted text. by default, highlighted text is wrapped
     *            in &lt;em&gt; and &lt;/em&gt;.
     * @param postTag
     *            use in conjunction with preTag to define the html tags to use
     *            for highlighted text. by default, highlighted text is wrapped
     *            in &lt;em&gt; and &lt;/em&gt;.
     * @param fragmentSize
     *            the size of the highlighted fragment in characters. default to
     *            100.
     * @param numOfFragments
     *            the maximum number of fragments to return. default to 5.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public ScmFulltextHighlightOption(String preTag, String postTag, int fragmentSize,
            int numOfFragments) throws ScmInvalidArgumentException {
        super();
        setTag(preTag, postTag);
        this.fragmentSize = fragmentSize;
        this.numOfFragments = numOfFragments;
    }

    /**
     * Specified the tag for highlighted text wrapped.
     * 
     * @param preTag
     *            use in conjunction with preTag to define the html tags to use
     *            for highlighted text. by default, highlighted text is wrapped
     *            in &lt;em&gt; and &lt;/em&gt;.
     * @param postTag
     *            use in conjunction with preTag to define the html tags to use
     *            for highlighted text. by default, highlighted text is wrapped
     *            in &lt;em&gt; and &lt;/em&gt;.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setTag(String preTag, String postTag) throws ScmInvalidArgumentException {
        if (preTag == null || postTag == null) {
            throw new ScmInvalidArgumentException(
                    "tag is null:pretag=" + preTag + ", posttag=" + postTag);
        }
        this.preTag = preTag;
        this.postTag = postTag;
    }

    /**
     * Get the preTag.
     * 
     * @return preTag.
     */
    public String getPreTag() {
        return preTag;
    }

    /**
     * Get the postTag.
     * 
     * @return postTag.
     */
    public String getPostTag() {
        return postTag;
    }

    /**
     * Get the fragment size
     * 
     * @return fragment size
     */
    public int getFragmentSize() {
        return fragmentSize;
    }

    /**
     * Set the size of the highlighted fragment in characters. default to 100.
     * 
     * @param fragmentSize
     *            fragment size.
     */
    public void setFragmentSize(int fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    /**
     * Get the number of fragments.
     * 
     * @return numOfFragments
     */
    public int getNumOfFragments() {
        return numOfFragments;
    }

    /**
     * Set the maximum number of fragments to return. default to 5.
     * 
     * @param numOfFragments
     *            numOfFragments.
     */
    public void setNumOfFragments(int numOfFragments) {
        this.numOfFragments = numOfFragments;
    }

}
