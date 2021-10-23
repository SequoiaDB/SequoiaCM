package com.sequoiacm.client.element;

import com.sequoiacm.client.common.ScmChecksumType;

/**
 * Option for create ScmBreakpointFile.
 */
public class ScmBreakpointFileOption {
    public static final int MIN_BREAKPOINT_SIZE = 1024 * 1024; // 1MB
    public static final int DEFAULT_BREAKPOINT_SIZE = 1024 * 1024 * 15; // 15MB

    private ScmChecksumType checksumType = ScmChecksumType.NONE;
    private int breakpointSize = DEFAULT_BREAKPOINT_SIZE;
    private boolean isNeedMd5 = false;

    /**
     * Create a new instance with default option.
     */
    public ScmBreakpointFileOption() {
    }

    /**
     * Create a new instance with specified options.
     * 
     * @param checksumType
     *            the checksum type of the breakpoint file
     * @param breakpointSize
     *            the upload breakpoint size
     * @param isNeedMd5
     *            is need calculate md5 for the breakpoint file.
     */
    public ScmBreakpointFileOption(ScmChecksumType checksumType, int breakpointSize,
            boolean isNeedMd5) {
        this.checksumType = checksumType;
        this.breakpointSize = Math.max(breakpointSize, MIN_BREAKPOINT_SIZE);
        this.isNeedMd5 = isNeedMd5;
    }

    /**
     * Get the checksum type of the breakpoint file
     * 
     * @return checksum type
     */
    public ScmChecksumType getChecksumType() {
        return checksumType;
    }

    /**
     * Set the checksum type of the breakpoint file
     * 
     * @param checksumType
     *            checksum type
     */
    public void setChecksumType(ScmChecksumType checksumType) {
        this.checksumType = checksumType;
    }

    /**
     * Get the upload breakpoint size
     * 
     * @return the upload breakpoint size
     */
    public int getBreakpointSize() {
        return breakpointSize;
    }

    /**
     * Set the upload breakpoint size
     * 
     * @param breakpointSize
     *            the upload breakpoint size
     */
    public void setBreakpointSize(int breakpointSize) {
        this.breakpointSize = Math.max(breakpointSize, MIN_BREAKPOINT_SIZE);
    }

    /**
     * Is need calculate md5 for the breakpoint file.
     * 
     * @return true or false.
     */
    public boolean isNeedMd5() {
        return isNeedMd5;
    }

    /**
     * Set need calculate md5 for the breakpoint file.
     * 
     * @param isNeedMd5
     *            is need calculate md5.
     */
    public void setNeedMd5(boolean isNeedMd5) {
        this.isNeedMd5 = isNeedMd5;
    }

}
