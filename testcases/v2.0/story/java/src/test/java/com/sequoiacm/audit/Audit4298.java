package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @Descreption SCM-4298:内容服务节点配置META_CLASS_DML|META_CLASS_DQL
 * @Author YiPan
 * @CreateDate 2022/5/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Audit4298 extends TestScmBase {
    private final String auditType = "META_CLASS_DML|META_CLASS_DQL";
    private final String metaClassName = "class4298";
    private final String metaAttrName = "attr4298";
    private final String desc = "desc4298";
    private ScmId attrId = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        WsWrapper wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test
    public void test() throws Exception {
        // 认证服务配置USER_DQL审计类型
        Map< String, String > confMap = new HashMap<>();
        confMap.put( ConfigCommonDefind.scm_audit_mask, auditType );
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        ConfUtil.updateConf( site.getSiteServiceName(), confMap );

        // 创建字定义元数据模板
        ScmFactory.Class.createInstance( ws, metaClassName, desc );
        ConfUtil.checkAuditByType( session, "CREATE_META_CLASS",
                metaClassName );

        // 查询字定义元数据模板
        ScmFactory.Class.getInstanceByName( ws, metaClassName );
        ConfUtil.checkAuditByType( session, "META_CLASS_DQL",
                "get class info with attr by className=" + metaClassName );

        // 创建字定义元数据属性
        attrId = ScmFactory.Attribute.createInstance( ws, new ScmAttributeConf()
                .setName( metaAttrName ).setType( AttributeType.STRING ) )
                .getId();
        ConfUtil.checkAuditByType( session, "CREATE_META_ATTR", metaAttrName );

        // 查询字定义元数据模板
        ScmFactory.Attribute.getInstance( ws, attrId );
        ConfUtil.checkAuditByType( session, "META_CLASS_DQL",
                "get attr info by attrId=" + attrId.get() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ConfUtil.deleteAuditConf( site.getSiteServiceName() );
                ScmFactory.Class.deleteInstanceByName( ws, metaClassName );
                ScmFactory.Attribute.deleteInstance( ws, attrId );
            }
        } finally {
            session.close();
        }
    }
}