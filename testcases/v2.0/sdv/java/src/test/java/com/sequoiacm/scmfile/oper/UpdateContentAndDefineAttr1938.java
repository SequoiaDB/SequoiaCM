package com.sequoiacm.scmfile.oper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: update Content,and defineAttributes testlink-case:SCM-1938
 *
 * @author wuyan
 * @Date 2018.07.10
 * @version 1.00
 */

public class UpdateContentAndDefineAttr1938 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "updatefile1938";
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @DataProvider(name = "testProvider")
    public Object[][] generatePageSize() {
        String attrStr1 = "{name:'attr1938_string', "
                + "display_name:'name1938_1', description:'Attribute 1938_1', "
                + "type:'STRING', required:true}";
        String attrStr2 = "{name:'attr1938_date', display_name:'name1938_2', "
                + "description:'Attribute 1938_2', type:'DATE', required:false}";
        String attrStr3 = "{name:'attr1938_double', "
                + "display_name:'name1938_3', description:'中文', type:'DOUBLE', "
                + "required:true}";
        String attrStr4 = "{name:'attr1938_int', display_name:'name1938_4', "
                + "description:'test int', type:'INTEGER', required:true}";
        String attrStr5 = "{name:'attr1938_bool', display_name:'name1938_5', "
                + "description:'test bool', type:'BOOLEAN', required:true}";
        return new Object[][] {
                // the parameter : className / attibutes / attrValue
                new Object[] { "class1938_1", attrStr1, "test1938" },
                new Object[] { "class1938_2", attrStr2,
                        "2018-07-10-01:01:00.000" },
                new Object[] { "class1938_3", attrStr3, 101.02 },
                new Object[] { "class1938_4", attrStr4, 120345 },
                new Object[] { "class1938_5", attrStr5, true }, };
    }

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        fileId = VersionUtils.createFileByFile( ws, fileName, filePath );
    }

    // SEQUOIACM-1143
    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "testProvider", enabled = false)
    private void test( String className, String attrStr, Object value )
            throws Exception {
        updateContentAndSetAttr( attrStr, value, className );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void updateContentAndSetAttr( String attrStr, Object value,
            String className ) throws Exception {
        // update content
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        byte[] testdata = new byte[ 1024 * 2 ];
        new Random().nextBytes( testdata );
        file.updateContent( new ByteArrayInputStream( testdata ) );

        // set property
        ScmAttribute attribute = createAttr( attrStr );
        ScmClass scmClass = ScmFactory.Class.createInstance( ws, className,
                "i am a class1938" );
        scmClass.attachAttr( attribute.getId() );
        ScmId scmClassId = scmClass.getId();
        ScmClassProperties properties = new ScmClassProperties(
                scmClassId.toString() );
        properties.addProperty( attribute.getName(), value );
        file.setClassProperties( properties );

        // check update content
        int majorVersion = file.getMajorVersion();
        VersionUtils.CheckFileContentByStream( ws, fileName, majorVersion,
                testdata );

        // check property
        checkSetPropertyResult( scmClassId, properties );

        // clean property and class
        ScmFactory.Class.deleteInstance( ws, scmClassId );
        ScmFactory.Attribute.deleteInstance( ws, attribute.getId() );
    }

    private void checkSetPropertyResult( ScmId scmClassId,
            ScmClassProperties expProperty ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getClassId(), scmClassId );
        Assert.assertEquals( file.getClassProperties().toString(),
                expProperty.toString() );
    }

    private ScmAttribute createAttr( String attrObjString )
            throws ScmException {
        BSONObject attrObj = ( BSONObject ) JSON.parse( attrObjString );
        ScmAttributeConf attr = new ScmAttributeConf();
        attr.setName( attrObj.get( "name" ).toString() );
        attr.setType(
                AttributeType.getType( attrObj.get( "type" ).toString() ) );
        attr.setDescription( attrObj.get( "description" ).toString() );
        attr.setDisplayName( attrObj.get( "display_name" ).toString() );
        attr.setCheckRule( null );
        attr.setRequired( ( boolean ) attrObj.get( "required" ) );
        return ScmFactory.Attribute.createInstance( ws, attr );
    }
}