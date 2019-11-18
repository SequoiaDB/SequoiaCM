package com.sequoiacm.testcommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.element.ScmSiteInfo;

public class SiteWrapper{
	// private static final Logger logger = Logger.getLogger(Site.class);
	private Random random = new Random();

	private ScmSiteInfo siteInfo;
	private List<NodeWrapper> nodes;
	private String serviceName;
    
	public SiteWrapper(List<NodeWrapper> allNodeList, ScmSiteInfo siteInfo) {
		this.siteInfo = siteInfo;
		nodes = this.getNodesOfSite(allNodeList, siteInfo.getId());
	}
	
	public int getNodeNum() {
		return nodes.size();
	}

	public int getSiteId() {
		return siteInfo.getId();
	}
	
	public String getSiteServiceName(){
		//return  serviceNameMap.get(getSiteId());
		this.serviceName = getSiteName().toLowerCase();
		return  serviceName;
	}
	
	public void setSiteServiceName(String serviceName){
		this.serviceName = serviceName;
	}

	public String getSiteName() {
		return siteInfo.getName();
	}

	public DatasourceType getDataType() {
		return siteInfo.getDataType();
	}

	public String getMetaUser() {
		return siteInfo.getMetaUser();
	}

	public String getMetaPasswd() /*throws ScmCryptoException */{
		String passwd = null;
				/*ScmPasswordMgr.getInstance().
			  decrypt(siteInfo.getMetaCryptType(), siteInfo.getMetaPasswd());*/
		return passwd;
	}

	public String getDataUser() {
		return siteInfo.getDataUser();
	}

	public String getDataPasswd() /*throws ScmCryptoException */{
		String passwd = null;
				/*ScmPasswordMgr.getInstance().
				decrypt(siteInfo.getDataCryptType(), siteInfo.getDataPasswd());*/
		return passwd;
	}

	public String getMetaDsUrl() {
		List<String> urls = this.getMetaDsUrls();
		String metaDsUrl = urls.get(random.nextInt(urls.size()));
		return metaDsUrl;
	}

	public String getDataDsUrl() {
		List<String> urls = this.getDataDsUrls();
		String dataDsUrl = urls.get(random.nextInt(urls.size()));
		return dataDsUrl;
	}

	public List<String> getMetaDsUrls() {
		return siteInfo.getMetaUrl();
	}

	public List<String> getDataDsUrls() {
		return siteInfo.getDataUrl();
	}

	public NodeWrapper getNode() {
		return this.getNodes(1).get(0);
	}

	/**
	 * get the specified number of nodes
	 */
	public List<NodeWrapper> getNodes(int num) {
		// check parameter
		int maxNodeNum = nodes.size();
		if (num > maxNodeNum) {
			throw new IllegalArgumentException("error, num > maxBranchSiteNum");
		}

		List<NodeWrapper> nodeList = new ArrayList<>();

		// get random number nodes
		int randNum = random.nextInt(maxNodeNum);
		nodeList.add(nodes.get(randNum));

		int addNum = randNum;
		for (int i = 1; i < num; i++) {
			addNum++;
			if (addNum < maxNodeNum) {
				nodeList.add(nodes.get(addNum));
			} else {
				nodeList.add(nodes.get(addNum - maxNodeNum));
			}
		}
		return nodeList;
	}
	
	/**
	 * get all the nodes of the current site
	 */
	/*public List<NodeWrapper> getNodes() {
		return this.getNodes(nodes.size());
	}*/

	/**
	 * get node info
	 */
	private List<NodeWrapper> getNodesOfSite(List<NodeWrapper> nodeList, int siteId) {
		List<NodeWrapper> nodesOfSite = new ArrayList<>();
		for (NodeWrapper node : nodeList) {
			int id = (int) node.getSiteId();
			if (id == siteId) {
				nodesOfSite.add(node);
			}
		}
		return nodesOfSite;
	}

	@Override
	public String toString() {
		return siteInfo + "\nnodes " + nodes + "\n";
	}
}
