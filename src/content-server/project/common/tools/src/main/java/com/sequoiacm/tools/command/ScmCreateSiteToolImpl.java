package com.sequoiacm.tools.command;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine.DataSourceType;
import com.sequoiacm.tools.ScmAdmin;
import com.sequoiacm.tools.common.RestDispatcher;
import com.sequoiacm.tools.common.ScmCommandUtil;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmDatasourceUtil;
import com.sequoiacm.tools.common.ScmHelpGenerator;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmSiteConfig;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exception.ScmToolsException;

public class ScmCreateSiteToolImpl implements ScmTool {
    private static final Logger logger = LoggerFactory
            .getLogger(ScmCreateSiteToolImpl.class.getName());
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_NAME = "n";

    private final String OPT_LONG_ROOT = "root";
    private final String OPT_SHORT_ROOT = "r";

    private final String OPT_LONG_CONTINUE = "continue";

    private final String OPT_LONG_DSURL = "dsurl";
    private final String OPT_LONG_DSUSER = "dsuser";
    private final String OPT_LONG_DSPASSWD = "dspasswd";
    private final String OPT_LONG_DSTYPE = "dstype";
    private final String OPT_LONG_DSCONF = "dsconf";
    private final String OPT_LONG_NO_CHECK_DS = "no-check-ds";

    private final String OPT_LONG_GATEWAY = "gateway";
    private final String OPT_LONG_AMDIN_USER = "user";
    private final String OPT_LONG_AMDIN_PASSWORD = "passwd";

    private Options options;
    private ScmHelpGenerator hp;

    public ScmCreateSiteToolImpl() throws ScmToolsException {
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "new site name.", true, true, false));
        options.addOption(hp.createOpt(OPT_SHORT_ROOT, OPT_LONG_ROOT, "set new site as root site.",
                false, false, false));
        options.addOption(hp.createOpt(null, OPT_LONG_CONTINUE,
                "when you are creating a root site, if the root site\ndatasource already have some meta data, you can set \n--"
                        + OPT_LONG_CONTINUE + " to complete the meta data.",
                false, false, true));

        options.addOption(hp.createOpt(null, OPT_LONG_DSTYPE,
                "data datasource type of new site, arg:[ 1 | 2 | 3 | 4 | 5 | 6 | 7 ],\n1:sequoiadb, 2:hbase, 3:ceph-s3, 4:ceph-swift, 5:hdfs, default:1.",
                false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_DSURL,
                "data datasource url of new sdb site, eg:'hostName1:port1,\nhostName2:port2'.\n",
                false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_DSCONF,
                "data datasource conf of new hdfs|hbase site, eg:'{\"fs.defaultFS\":\"hdfs://hostName1:port1\",...}' .\n",
                false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_DSUSER,
                "data datasource username of new site, default:empty string.", false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_DSPASSWD,
                "data datasource password of new site, default:empty string.", false, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_NO_CHECK_DS,
                "do not check datasource is available", false, false, false));
        options.addOption(hp.createOpt(null, OPT_LONG_GATEWAY,
                "gateway url, eg:'host1:port,host2:port,host3:port'.", true, true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_AMDIN_USER, "login admin username.", true,
                true, false));
        options.addOption(hp.createOpt(null, OPT_LONG_AMDIN_PASSWORD, "login admin password.", true,
                true, false));
        ScmCommandUtil.addDsOption(options, hp);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmAdmin.checkHelpArgs(args);

        CommandLine cl = ScmCommandUtil.parseArgs(args, options);
        boolean isRootSite;
        ScmSdbInfo metaDsInfo = null;
        ScmSdbInfo dataDsInfo = null;

        if (cl.hasOption(OPT_SHORT_ROOT)) {
            // for backupwards compatibility
            if (cl.hasOption(ScmCommandUtil.OPT_LONG_RDSURL)) {
                metaDsInfo = parseSdbInfoOps(ScmCommandUtil.OPT_LONG_RDSURL,
                        ScmCommandUtil.OPT_LONG_RDSUSER, ScmCommandUtil.OPT_LONG_RDSPASSWD, null,
                        cl);
            }
            else {
                metaDsInfo = parseSdbInfoOps(ScmCommandUtil.OPT_LONG_MDSURL,
                        ScmCommandUtil.OPT_LONG_MDSUSER, ScmCommandUtil.OPT_LONG_MDSPASSWD, null,
                        cl);
            }
            if (metaDsInfo == null) {
                throw new ScmToolsException("missing options:--" + ScmCommandUtil.OPT_LONG_MDSURL
                        + ",--" + ScmCommandUtil.OPT_LONG_MDSUSER + ",--"
                        + ScmCommandUtil.OPT_LONG_MDSPASSWD, ScmExitCode.INVALID_ARG);
            }
            isRootSite = true;
        }
        else {
            if (cl.hasOption(OPT_LONG_CONTINUE)) {
                throw new ScmToolsException("--" + OPT_LONG_CONTINUE + " only for create root site",
                        ScmExitCode.INVALID_ARG);
            }
            isRootSite = false;
        }

        dataDsInfo = parseSdbInfoOps(OPT_LONG_DSURL, OPT_LONG_DSUSER, OPT_LONG_DSPASSWD,
                OPT_LONG_DSTYPE, cl);
        if (dataDsInfo == null) {
            throw new ScmToolsException("missing options:--" + OPT_LONG_DSURL + ",--"
                    + OPT_LONG_DSUSER + ",--" + OPT_LONG_DSPASSWD, ScmExitCode.INVALID_ARG);
        }

        String siteName = cl.getOptionValue(OPT_SHORT_NAME);
        // tranform DatasourceType
        DatasourceType dataType = getDatasourceType(cl.getOptionValue(OPT_LONG_DSTYPE));
        // if dataType is hdfs|hbase newSite setDataConf
        Map<String, String> dataConf = getDataConf(cl, dataType.getType());

        ScmSession ss = null;
        try {
            ScmSiteConfig siteConf = null;
            if (metaDsInfo == null) {
                siteConf = ScmSiteConfig.start(siteName).isRootSite(isRootSite)
                        .SetDataSourceType(dataType).setDataSource(dataDsInfo.getSdbUrlList(),
                                dataDsInfo.getSdbUser(), dataDsInfo.getSdbPasswd(), dataConf)
                        .build();
            }
            else {
                siteConf = ScmSiteConfig.start(siteName).isRootSite(isRootSite)
                        .SetDataSourceType(dataType)
                        .setDataSource(dataDsInfo.getSdbUrlList(), dataDsInfo.getSdbUser(),
                                dataDsInfo.getSdbPasswd(), dataConf)
                        .setMetaSource(metaDsInfo.getSdbUrlList(), metaDsInfo.getSdbUser(),
                                metaDsInfo.getSdbPasswd())
                        .build();
            }
            // TODO；config server check dbUrl
            // check data url is connectable
            if (!cl.hasOption(OPT_LONG_NO_CHECK_DS)) {
                // ScmSiteConf transform ScmSiteInfo
                ScmSiteInfo siteInfo = transformSiteInfo(siteConf);
                // id is not practical meaning
                siteInfo.setId(1);
                ScmDatasourceUtil.vlidateDatasourceUrl(siteInfo);
            }

            String urls = cl.getOptionValue(OPT_LONG_GATEWAY);
            String user = cl.getOptionValue(OPT_LONG_AMDIN_USER);
            String pwd = cl.getOptionValue(OPT_LONG_AMDIN_PASSWORD);
            ss = ScmFactory.Session.createSession(
                    new ScmConfigOption(ScmCommandUtil.parseListUrls(urls), user, pwd));
            RestDispatcher.getInstance().createSite(ss, siteConf);
            System.out.println("create site success:siteName=" + siteName);
        }
        catch (ScmException e) {
            logger.error("create site failed:siteName={}, error=", siteName, e.getError(), e);
            throw new ScmToolsException("create site failed:error=" + e.getError(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(ss);
        }
    }

    private ScmSiteInfo transformSiteInfo(ScmSiteConfig siteConf)
            throws ScmInvalidArgumentException, ScmToolsException {
        ScmSiteInfo siteInfo = new ScmSiteInfo();
        siteInfo.setName(siteConf.getName());
        siteInfo.setRootSite(siteConf.isRootSite());
        siteInfo.setDataType(siteConf.getDataType().getType());
        siteInfo.setDataUrl(siteConf.getDataUrl());
        siteInfo.setDataUser(siteConf.getDataUser());
        siteInfo.setDataPasswd(siteConf.getDataPassword());
        siteInfo.setDataConf(siteConf.getDataConfig());
        siteInfo.setMetaUrl(siteConf.getMetaUrl());
        siteInfo.setMetaUser(siteConf.getMetaUser());
        siteInfo.setMetaPasswd(siteConf.getMetaPassword());
        return siteInfo;
    }

    private Map<String, String> getDataConf(CommandLine cl, String dataType)
            throws ScmToolsException {
        if (dataType.equals(DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR)
                || dataType.equals(DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR)) {
            return parseDataConf(cl);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseDataConf(CommandLine cl) throws ScmToolsException {
        Map<String, String> map = new HashMap<String, String>();
        if (cl.hasOption(OPT_LONG_DSCONF)) {
            String optionValue = cl.getOptionValue(OPT_LONG_DSCONF);
            map = (Map<String, String>) JSON.parse(optionValue);
        }
        else {
            throw new ScmToolsException("missing options:--" + OPT_LONG_DSCONF,
                    ScmExitCode.INVALID_ARG);
        }

        return map;
    }

    private DatasourceType getDatasourceType(String optionValue) throws ScmToolsException {
        if (optionValue == null) {
            return DatasourceType.SEQUOIADB;
        }
        switch (optionValue) {
            case "1":
                return DatasourceType.SEQUOIADB;
            case "2":
                return DatasourceType.HBASE;
            case "3":
                return DatasourceType.CEPH_S3;
            case "4":
                return DatasourceType.CEPH_SWIFT;
            case "5":
                return DatasourceType.HDFS;
            default:
                throw new ScmToolsException("Unknown datasource type:" + optionValue,
                        ScmExitCode.INVALID_ARG);
        }
    }

    private ScmSdbInfo parseSdbInfoOps(String urlOpName, String userOpName, String passwdOpName,
            String dsTypeOpName, CommandLine cl) throws ScmToolsException {
        if (cl.hasOption(urlOpName) || dsTypeIsHadoop(cl, dsTypeOpName)) {
            ScmSdbInfo info = new ScmSdbInfo();
            info.setSdbUrl(
                    cl.getOptionValue(urlOpName) == null ? "" : cl.getOptionValue(urlOpName));
            if (cl.hasOption(passwdOpName) && cl.hasOption(userOpName)) {
                info.setSdbPasswd(cl.getOptionValue(passwdOpName));
                info.setSdbUser(cl.getOptionValue(userOpName));
            }
            else if (!cl.hasOption(userOpName) && !cl.hasOption(passwdOpName)) {
                info.setSdbUser("");
                info.setSdbPasswd("");
            }
            else {
                throw new ScmToolsException(
                        "Please set " + userOpName + " and " + passwdOpName + " at the same time",
                        ScmExitCode.INVALID_ARG);
            }
            return info;
        }
        else if (cl.hasOption(userOpName) || cl.hasOption(passwdOpName)) {
            throw new ScmToolsException("missing option : --" + urlOpName, ScmExitCode.INVALID_ARG);
        }
        return null;
    }

    private boolean dsTypeIsHadoop(CommandLine cl, String dsTypeOpName) throws ScmToolsException {
        if (dsTypeOpName == null) {
            return false;
        }
        else {
            String dsType = cl.getOptionValue(dsTypeOpName);
            if (dsType == null) {
                return false;
            }
            switch (dsType) {
                case "1":
                    return false;
                case "2":
                    return true;
                case "3":
                    return false;
                case "4":
                    return false;
                case "5":
                    return true;
                case "6":
                    return true;
                case "7":
                    return true;
                default:
                    throw new ScmToolsException("Unknown datasource type:" + dsType,
                            ScmExitCode.INVALID_ARG);
            }
        }
    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }
}
