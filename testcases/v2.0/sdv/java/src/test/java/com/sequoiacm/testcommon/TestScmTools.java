package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
public class TestScmTools extends TestScmBase {
	
	/**
	 * create session, by specified site
	 */
	public static ScmSession createSession(SiteWrapper site) throws ScmException {
		return createSession(site.getSiteServiceName());
	}
	
	/**
	 * create session, by specified site and user
	 */
	public static ScmSession createSession(SiteWrapper site, String username, String password) throws ScmException {
		return createSession(site.getSiteServiceName(), username, password);
	}
	
	/**
	 * create session by specified serviceName
	 */
	public static ScmSession createSession(String serviceName) throws ScmException {
		return createSession(serviceName, TestScmBase.scmUserName, TestScmBase.scmPassword);
	}
	
	/**
	 * create session by specified serviceName and user
	 */
	public static ScmSession createSession(String serviceName, String username, String password) throws ScmException {
		List<String> urlList = new ArrayList<String>();
		for (String gateway : gateWayList) {
			urlList.add(gateway + "/" + serviceName);
		}
		ScmConfigOption scOpt = new ScmConfigOption(urlList, username, password);
		ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, scOpt);
		return session;
	}

	/**
	 * create session by random serviceName
	 */
	public static ScmSession createSession() throws ScmException {
		List<String> urlList = new ArrayList<String>();
		SiteWrapper site = ScmInfo.getSite();
		for (String gateway : gateWayList) {
			urlList.add(gateway + "/" + site.getSiteServiceName());
		}
		ScmConfigOption scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName, TestScmBase.scmPassword);
		ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, scOpt);
		return session;
	}
}
