package com.sequoiacm.testcommon.listener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import com.sequoiacm.testresource.SkipTestException;

public class TestScmListener extends TestListenerAdapter {
    private static final Logger logger = Logger
            .getLogger( TestScmListener.class );
    public static List< String > skipTests = Collections
            .synchronizedList( new ArrayList< String >() );
    private static final String[] testPubClass = {
            "java.util.List<com.sequoiacm.testcommon.SiteWrapper>",
            "class com.sequoiacm.testcommon.SiteWrapper",
            "java.util.List<com.sequoiacm.testcommon.WsWrapper>",
            "class com.sequoiacm.testcommon.WsWrapper",
            "class com.sequoiacm.client.core.ScmSession" };

    /**
     * print case end time
     */
    @Override
    public void onConfigurationSuccess( ITestResult itr ) {
        super.onConfigurationSuccess( itr );
        if ( itr.getMethod().isAfterClassConfiguration() ) {
            logger.info( "[" + itr.getInstanceName() + "] end" );
        }
        if ( itr.getMethod().isBeforeClassConfiguration() ) {
            try {
                getScmInfo( itr );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * print case end time
     */
    @Override
    public void onConfigurationFailure( ITestResult itr ) {
        super.onConfigurationFailure( itr );
        if ( itr.getMethod().isAfterClassConfiguration() ) {
            logger.info( "[" + itr.getInstanceName() + "] end" );
        }
        if ( itr.getMethod().isBeforeClassConfiguration() ) {
            try {
                getScmInfo( itr );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * print case end time
     */
    @Override
    public void onConfigurationSkip( ITestResult itr ) {
        super.onConfigurationSkip( itr );
        if ( itr.getMethod().isAfterClassConfiguration() ) {
            logger.info( "[" + itr.getInstanceName() + "] end" );
        }
    }

    /**
     * print case start time
     */
    @Override
    public void beforeConfiguration( ITestResult itr ) {
        super.beforeConfiguration( itr );
        if ( itr.getMethod().isBeforeClassConfiguration() ) {
            logger.info( "[" + itr.getInstanceName() + "] start" );
        }
    }

    private void getScmInfo( ITestResult result )
            throws IllegalArgumentException, IllegalAccessException {
        String className = result.getInstanceName();
        Field[] fields = result.getInstance().getClass().getDeclaredFields();
        for ( Field field : fields ) {
            field.setAccessible( true );
            String type = field.getGenericType().toString();
            Object info = field.get( result.getInstance() );
            for ( int j = 0; j < testPubClass.length; j++ ) {
                if ( type.equals( testPubClass[ j ] ) ) {
                    logger.info( "[" + className + "] " + field.getName()
                            + " info \n[" + info + "]" );
                }
            }
        }
    }

    @Override
    public void onTestSkipped( ITestResult iTestResult ) {
        String skipTag = SkipTestException.getSkipTag();
        Throwable throwable = iTestResult.getThrowable();
        if ( !throwable.toString().contains( skipTag ) ) {
            skipTests.add( iTestResult.getTestClass().getName() );
        }
        super.onTestSkipped( iTestResult );
    }
}
