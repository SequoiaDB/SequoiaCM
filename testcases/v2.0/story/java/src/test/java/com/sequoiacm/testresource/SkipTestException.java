package com.sequoiacm.testresource;

import org.testng.SkipException;

/**
 * @author
 * @descreption
 * @date
 * @updateUser
 * @updateDate 2023/2/15
 * @updateRemark
 */
public class SkipTestException extends SkipException {
    private static String skipTag = "Test skip:";

    public static String getSkipTag() {
        return skipTag;
    }

    public SkipTestException( String s ) {
        super( skipTag + s );
    }

    public SkipTestException( String s, Throwable throwable ) {
        super( skipTag + s, throwable );
    }
}
