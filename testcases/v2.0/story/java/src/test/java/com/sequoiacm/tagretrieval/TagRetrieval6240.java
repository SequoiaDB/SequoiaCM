// package com.sequoiacm.tagretrieval;
//
// import com.sequoiacm.client.core.ScmSession;
// import com.sequoiacm.client.core.ScmWorkspace;
// import com.sequoiacm.client.exception.ScmException;
// import com.sequoiacm.common.ScmWorkspaceTagRetrievalStatus;
// import com.sequoiacm.testcommon.*;
// import com.sequoiacm.testcommon.listener.GroupTags;
// import com.sequoiacm.testcommon.scmutils.TagRetrievalUtils;
// import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
// import com.sequoiadb.base.DBCursor;
// import com.sequoiadb.base.Sequoiadb;
// import org.bson.BSONObject;
// import org.bson.BasicBSONObject;
// import org.testng.Assert;
// import org.testng.annotations.AfterClass;
// import org.testng.annotations.BeforeClass;
// import org.testng.annotations.Test;
// import java.util.ArrayList;
// import java.util.List;
//
/// **
// * @Descreption SCM-6240 :: 创建工作区指定标签检索和domain
// * @Author fangjun
// * @CreateDate
// * @UpdateUser
// * @UpdateDate 2023/5/18
// * @UpdateRemark
// * @Version 1.0
// */
// public class TagRetrieval6240 extends TestScmBase {
// private boolean runSuccess = false;
// private ScmSession session = null;
// private ScmWorkspace ws1 = null;
// private ScmWorkspace ws2 = null;
// private ScmWorkspace ws3 = null;
// private ScmWorkspace ws4 = null;
// private String domainName = "domain6240";
// private SiteWrapper rootSite = null;
// private String wsName1 = "ws6240_1";
// private String wsName2 = "ws6240_2";
// private String wsName3 = "ws6240_3";
// private String wsName4 = "ws6240_4";
//
// @BeforeClass
// private void setUp() throws Exception {
// rootSite = ScmInfo.getRootSite();
// session = ScmSessionUtils.createSession( rootSite );
// ScmWorkspaceUtil.deleteWs( wsName1, session );
// ScmWorkspaceUtil.deleteWs( wsName2, session );
// ScmWorkspaceUtil.deleteWs( wsName3, session );
// ScmWorkspaceUtil.deleteWs( wsName4, session );
// deleteDomain( rootSite, domainName );
// createDomain( rootSite, domainName );
// }
//
// @Test(groups = { GroupTags.base })
// private void test() throws ScmException, InterruptedException {
// int siteNum = ScmInfo.getSiteNum();
// ws1 = ScmWorkspaceUtil.createWS( session, wsName1, true, siteNum );
// ws2 = ScmWorkspaceUtil.createWS( session, wsName2, siteNum );
// ws3 = ScmWorkspaceUtil.createWS( session, wsName3, siteNum,
// domainName );
// ws4 = ScmWorkspaceUtil.createWS( session, wsName4, true, siteNum );
// ScmWorkspaceUtil.wsSetPriority( session, wsName1 );
// ScmWorkspaceUtil.wsSetPriority( session, wsName2 );
// ScmWorkspaceUtil.wsSetPriority( session, wsName3 );
// ScmWorkspaceUtil.wsSetPriority( session, wsName4 );
// Assert.assertEquals( ws1.getTagRetrievalStatus(),
// ScmWorkspaceTagRetrievalStatus.ENABLED );
// Assert.assertEquals( ws2.getTagRetrievalStatus(),
// ScmWorkspaceTagRetrievalStatus.DISABLED );
// Assert.assertEquals( ws3.getTagLibMetaOption().getTagLibDomain(),
// domainName );
// Sequoiadb sdb = TestSdbTools.getSdb( TestScmBase.mainSdbUrl );
//
// DBCursor query = sdb
// .getCollectionSpace( TagRetrievalUtils.SCM_SYSTEM_CS )
// .getCollection( TagRetrievalUtils.GLOBAL_CONFIG_CL ).query();
// String defaultDomain = null;
// if ( query.hasNext() ) {
// defaultDomain = ( String ) query.getNext().get( "config_value" );
// }
// Assert.assertEquals( ws4.getTagLibMetaOption().getTagLibDomain(),
// defaultDomain );
// runSuccess = true;
// }
//
// @AfterClass
// private void tearDown() throws Exception {
// if ( runSuccess || TestScmBase.forceClear ) {
// ScmWorkspaceUtil.deleteWs( wsName1, session );
// ScmWorkspaceUtil.deleteWs( wsName2, session );
// ScmWorkspaceUtil.deleteWs( wsName3, session );
// ScmWorkspaceUtil.deleteWs( wsName4, session );
// deleteDomain( rootSite, domainName );
// if ( session != null ) {
// session.close();
// }
// }
// }
//
// private void createDomain( SiteWrapper site, String domainName ) {
// Sequoiadb sdb = null;
// try {
// sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
// List< String > groupNameList = getGroupNames( sdb );
// BSONObject obj = new BasicBSONObject();
// obj.put( "Groups", groupNameList.toArray() );
// if ( sdb.isDomainExist( domainName ) ) {
// sdb.dropDomain( domainName );
// sdb.createDomain( domainName, obj );
// } else {
// sdb.createDomain( domainName, obj );
// }
// } finally {
// if ( sdb != null ) {
// sdb.close();
// }
// }
// }
//
// public void deleteDomain( SiteWrapper site, String domainName ) {
// Sequoiadb sdb = null;
// DBCursor dbCursor = null;
// try {
// sdb = TestSdbTools.getSdb( site.getDataDsUrl() );
// if ( sdb.isDomainExist( domainName ) ) {
// dbCursor = sdb.getDomain( domainName ).listCSInDomain();
// String cs = null;
// while ( dbCursor.hasNext() ) {
// cs = ( String ) dbCursor.getNext().get( "Name" );
// }
// sdb.dropCollectionSpace( cs );
// sdb.dropDomain( domainName );
// }
// } finally {
// if ( dbCursor != null ) {
// dbCursor.close();
// }
// if ( sdb != null ) {
// sdb.close();
// }
//
// }
// }
//
// private List< String > getGroupNames( Sequoiadb db ) {
// List< String > groupNameList = db.getReplicaGroupNames();
// List< String > sysGroupname = new ArrayList< String >();
// int num = groupNameList.size();
// for ( int i = 0; i < num; i++ ) {
// if ( groupNameList.get( i ).contains( "SYS" ) ) {
// sysGroupname.add( groupNameList.get( i ) );
// }
// }
// groupNameList.removeAll( sysGroupname );
// return groupNameList;
// }
//
// }
