package com.sequoiacm.cloud.tools.command;

import com.sequoiacm.cloud.tools.common.*;

import com.sequoiacm.infrastructure.tool.command.ScmCreateNodeToolImpl;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmNodeCreator;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeRequiredParamGroup;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;

import java.util.*;

public class ScmCreateNodeToolImplCloud extends ScmCreateNodeToolImpl {
    private final String OPT_LONG_AUDIT_URL = "adurl";
    private final String OPT_LONG_AUDIT_USER = "aduser";
    private final String OPT_LONG_AUDIT_PASSWD = "adpasswd";

    public ScmCreateNodeToolImplCloud(Map<String, ScmNodeRequiredParamGroup> nodeType2RequireParams,
            ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super(nodeType2RequireParams, nodeTypes);
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

        ScmNodeType nodeType = this.nodeTypes.getNodeTypeByStr(nodeTypeStr);

        Properties nodeConf;
        if (cl.hasOption(OPT_SHORT_CUSTOM_PROP)) {
            nodeConf = cl.getOptionProperties(OPT_SHORT_CUSTOM_PROP);
        }
        else {
            nodeConf = new Properties();
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
        ScmNodeCreator creator = new ScmNodeCreator(nodeType, nodeConf, nodeTypes, otherLog, false);
        creator.create();
    }
}
