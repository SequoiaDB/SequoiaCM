package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4295 :: 内容服务节点配置删除的审计类型
 * @author Zhaoyujing
 * @Date 2020/6/2
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Audit4295 extends TestScmBase {
    private ScmSession session = null;
    private final String metaClassName = "class4295";
    private final String metaAttrName = "attr4295";
    private final String desc = "desc4295";
    private ScmId attrId = null;
    private String contentServiceName = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        contentServiceName = site.getSiteServiceName();

        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        cleanAttrClass();

        ConfUtil.deleteAuditConf( contentServiceName );
    }

    @Test
    public void test() throws Exception {
        Map< String, String > attrDmlConfMap = new HashMap<>();
        attrDmlConfMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        attrDmlConfMap.put( ConfigCommonDefind.scm_audit_mask,
                "META_ATTR_DML|META_ATTR_DQL" );
        ConfUtil.updateConf( contentServiceName, attrDmlConfMap );

        // 创建字定义元数据模板
        ScmFactory.Class.createInstance( ws, metaClassName, desc );
        Assert.assertFalse( ConfUtil.checkAuditByType( session,
                "CREATE_META_CLASS", metaClassName ) );

        // 查询字定义元数据模板
        ScmFactory.Class.getInstanceByName( ws, metaClassName );
        Assert.assertFalse( ConfUtil.checkAuditByType( session,
                "META_CLASS_DQL",
                "get class info with attr by className=" + metaClassName ) );

        // 创建字定义元数据属性
        attrId = ScmFactory.Attribute.createInstance( ws, new ScmAttributeConf()
                .setName( metaAttrName ).setType( AttributeType.STRING ) )
                .getId();
        Assert.assertFalse( ConfUtil.checkAuditByType( session,
                "CREATE_META_ATTR", metaAttrName ) );

        // 查询字定义元数据模板
        ScmFactory.Attribute.getInstance( ws, attrId );
        Assert.assertFalse( ConfUtil.checkAuditByType( session, "META_ATTR_DQL",
                "get attr info by attrId=" + attrId.get() ) );

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ConfUtil.deleteAuditConf( contentServiceName );
                ScmFactory.Class.deleteInstanceByName( ws, metaClassName );
                ScmFactory.Attribute.deleteInstance( ws, attrId );
            }
        } finally {
            session.close();
        }
    }

    private void cleanAttrClass() throws ScmException {
        BSONObject attrMatcher = new BasicBSONObject();
        attrMatcher.put( ScmAttributeName.Attribute.NAME, metaAttrName );
        ScmCursor< ScmAttribute > attrCursor = ScmFactory.Attribute
                .listInstance( ws, attrMatcher );
        while ( attrCursor.hasNext() ) {
            ScmFactory.Attribute.deleteInstance( ws,
                    attrCursor.getNext().getId() );
        }
        BSONObject classMatcher = new BasicBSONObject();
        classMatcher.put( ScmAttributeName.Attribute.NAME, metaClassName );
        ScmCursor< ScmClassBasicInfo > classCursor = ScmFactory.Class
                .listInstance( ws, classMatcher );
        while ( classCursor.hasNext() ) {
            ScmFactory.Class.deleteInstance( ws,
                    classCursor.getNext().getId() );
        }
    }
}
