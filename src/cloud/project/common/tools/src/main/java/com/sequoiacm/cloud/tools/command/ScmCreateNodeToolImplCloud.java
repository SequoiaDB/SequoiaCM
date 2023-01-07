package com.sequoiacm.cloud.tools.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.sequoiacm.cloud.tools.common.ScmSysTableCreator;
import com.sequoiacm.cloud.tools.common.ScmSysTableProcessorFactory;
import com.sequoiacm.cloud.tools.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmNodeCreator;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;

public class ScmCreateNodeToolImplCloud extends ScmCreateNodeToolImpl {

    private static final Logger logger = LoggerFactory.getLogger(ScmCreateNodeToolImplCloud.class);
    private final String OPT_LONG_AUDIT_URL = "adurl";
    private final String OPT_LONG_AUDIT_USER = "aduser";
    private final String OPT_LONG_AUDIT_PASSWD = "adpasswd";

    public ScmCreateNodeToolImplCloud(Map<String, ScmNodeRequiredParamGroup> nodeType2RequireParams,
            List<ScmServiceNodeOperator> operators) throws ScmToolsException {
        super(nodeType2RequireParams, operators);
        ops.addOption(
                hp.createOpt(null, OPT_LONG_AUDIT_URL, "audit to sdb url.", true, true, false));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_AUDIT_USER, "audit to sdb user.", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AUDIT_PASSWD, "audit to sdb passwd.", true, true,
                false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmCommandUtil.parseArgs(args, ops);
        String nodeTypeStr = cl.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE);

        ScmNodeType nodeType = super.nodeOeprators.getSupportTypes().getNodeTypeByStr(nodeTypeStr);

        Properties nodeConf;
        if (cl.hasOption(OPT_SHORT_CUSTOM_PROP)) {
            nodeConf = cl.getOptionProperties(OPT_SHORT_CUSTOM_PROP);
        }
        else {
            nodeConf = new Properties();
        }

        if (nodeType.getName().equals(ScmNodeTypeEnum.SERVICETRACE.getName())
                && isExistNodeInServiceCenter(nodeConf, nodeType)) {
            throw new ScmToolsException(
                    "The number of service-trace nodes exceeds the limit num: 1",
                    ScmExitCode.SYSTEM_ERROR);
        }
        ScmSysTableCreator sysTableCreator = ScmSysTableProcessorFactory
                .getSysTableCreator(nodeType, nodeConf);
        if (sysTableCreator != null) {
            sysTableCreator.create();
        }

        if (nodeConf != null) {
            ScmNodeRequiredParamGroup scmNodeRequiredParamGroup = nodeType2RequireParams.get(nodeType.getType());
            if (scmNodeRequiredParamGroup != null) {
                scmNodeRequiredParamGroup.check(nodeConf);
            }
        }
        String adurl = cl.getOptionValue(OPT_LONG_AUDIT_URL);
        String aduser = cl.getOptionValue(OPT_LONG_AUDIT_USER);
        String adpasswd = cl.getOptionValue(OPT_LONG_AUDIT_PASSWD);
        logger.info("adurl="+adurl+", aduser="+aduser);

        Map<Object, Object> otherLog = new HashMap<>();
        otherLog.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_URL, adurl);
        otherLog.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_USER, aduser);
        otherLog.put(ScmToolsDefine.PROPERTIES.LOG_AUDIT_SDB_PASSWD, adpasswd);
        ScmNodeCreator creator = new ScmNodeCreator(nodeType, nodeConf, super.nodeOeprators,
                otherLog, false);
        creator.create();
    }

    private boolean isExistNodeInServiceCenter(Properties nodeConf, ScmNodeType nodeType) {
        Set<String> eurekaUrls = ScmCommon.getEurekaUrlsFromConfig(nodeConf);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        for (String eurekaUrl : eurekaUrls) {
            try {
                restTemplate.getForObject(eurekaUrl + "apps/" + nodeType.getName(), String.class);
                // 正常返回表示服务存在
                return true;
            }
            catch (HttpClientErrorException e) {
                // 出现 404 错误表示服务不存在
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return false;
                }
                // 其它错误时重试下一个eureka节点
            }
            catch (Exception ignored) {
            }
        }
        // 所有 eureka 节点都不可用，不中断节点创建流程
        logger.warn("failed to check {} count: no service-center node available",
                nodeType.getType());
        return false;
    }
}
