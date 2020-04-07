package com.sequoiacm.session.seria;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-527: User表为空
 * @Author linsuqiang
 * @Date 2017-06-25
 * @Version 1.00
 */

/*
 * 1、SCMSYSTEM.USER表为空； 2、登入SCM，检查登入结果；
 */

public class LoginWhenUserClEmpty527 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( LoginWhenUserClEmpty527.class );
    private static SiteWrapper site = null;

    private Sequoiadb sdb = null;
    private DBCollection userCL = null;
    private List< BSONObject > oldUserRecs = new ArrayList< BSONObject >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();

            sdb = new Sequoiadb( TestScmBase.mainSdbUrl,
                    TestScmBase.sdbUserName, TestScmBase.sdbPassword );
            userCL = sdb.getCollectionSpace( TestSdbTools.SCM_CS )
                    .getCollection( TestSdbTools.SCM_CL_USER );

            saveAndClearCL( userCL );
        } catch ( BaseException e ) {
            e.printStackTrace();
            if ( sdb != null ) {
                sdb.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/" + site,
                    TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION, scOpt );
            Assert.fail( "login shouldn't succeed when user cl is empty!" );
        } catch ( ScmException e ) {
            if ( -301 !=
                    e.getErrorCode() ) { // EN_SCM_BUSINESS_LOGIN_FAILED(-301)
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            restoreCL( userCL );
        } catch ( BaseException e ) {
            logger.error( "fail to restore userCL, original records: " +
                    oldUserRecs );
            Assert.fail( e.getMessage() );
        } finally {
            if ( sdb != null ) {
                sdb.close();
            }
        }

    }

    private void saveAndClearCL( DBCollection cl ) {
        // save user table to restore then
        DBCursor cursor = null;
        try {
            cursor = userCL.query();
            while ( cursor.hasNext() ) {
                oldUserRecs.add( cursor.getNext() );
            }
        } finally {
            cursor.close();
        }

        // delete all records
        userCL.delete( "{}" );
    }

    private void restoreCL( DBCollection cl ) {
        for ( BSONObject rec : oldUserRecs ) {
            cl.insert( rec );
        }
    }

}