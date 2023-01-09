package com.sequoiacm.deploy.deployer;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.module.ElasticsearchInfo;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.deploy.common.BsonUtils;
import com.sequoiacm.deploy.common.DeployJsonDefine;
import com.sequoiacm.deploy.core.ScmPasswordFileSender;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;

import java.io.File;

@Deployer
public class FulltextServerDeployer extends ServiceDeployerBase {

    private ScmPasswordFileSender pwdFileSender = ScmPasswordFileSender.getInstance();;
    private String esPwdFile;
    private String esCertPath;

    public FulltextServerDeployer() {
        super(ServiceType.FULLTEXT_SERVER);
    }

    @Override
    protected void beforeDeploy(NodeInfo node) throws Exception {
        super.beforeDeploy(node);
        HostInfo hostInfo = super.getDeployInfoMgr().getHostInfoWithCheck(node.getHostName());
        pwdFileSender.sendMetasourcePasswdFile(hostInfo, getDeployInfoMgr().getMetasourceInfo());
        ElasticsearchInfo esInfo = getDeployInfoMgr().getEsInfo();
        if (esInfo.getPassword() != null && esInfo.getPassword().trim().length() > 0) {
            esPwdFile = pwdFileSender.sendPlaintextAsPasswordFile(hostInfo, esInfo.getUser(),
                    esInfo.getPassword(), "elasticsearch.pwd");
        }
        if (esInfo.getCertPath() != null && esInfo.getCertPath().trim().length() > 0) {
            esCertPath = pwdFileSender.sendFile(hostInfo, esInfo.getCertPath(),
                    new File(esInfo.getCertPath()).getName());
        }
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) throws Exception {
        BasicBSONList templateBSONArray = BsonUtils.getArrayChecked(templateBson,
                DeployJsonDefine.FULLTEXT_SERVER);
        BSONObject templateServerBson = (BSONObject) templateBSONArray.get(0);

        BasicBSONList decoratedArray = new BasicBSONList();
        BasicBSONObject decoratedBSON = new BasicBSONObject();
        decoratedBSON.putAll(templateServerBson);
        decoratedBSON.putAll(super.genBaseDeployJson(node));

        decoratedBSON.put(DeployJsonDefine.ZOOKEEPER_URL, getDeployInfoMgr().getZkUrls());
        ElasticsearchInfo esInfo = getDeployInfoMgr().getEsInfo();
        decoratedBSON.put(DeployJsonDefine.FULL_TEXT_ES_URL, esInfo.getUrl());
        if (esInfo.getUser() != null) {
            decoratedBSON.put(DeployJsonDefine.FULL_TEXT_ES_USER, esInfo.getUser());
        }
        if (esPwdFile != null) {
            decoratedBSON.put(DeployJsonDefine.FULL_TEXT_ES_PASSWORD, esPwdFile);
        }
        if (esCertPath != null) {
            decoratedBSON.put(DeployJsonDefine.FULL_TEXT_ES_CERT_PATH, esCertPath);
        }
        decoratedArray.add(decoratedBSON);
        return new BasicBSONObject().append(DeployJsonDefine.FULLTEXT_SERVER, decoratedArray);
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceInstallPath,
            String deployJsonFileRemotePath) {
        return serviceInstallPath + "/bin/ftctl.sh start --timeout " + CommonUtils.getWaitServiceReadyTimeout()
                + " -p " + node.getPort();
    }
}
