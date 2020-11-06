package com.sequoiacm.tools.command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.ScmCtl;
import com.sequoiacm.tools.common.PropertiesUtil;
import com.sequoiacm.tools.common.RestDispatcher;
import com.sequoiacm.tools.common.ScmCommon;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmMetaMgr;
import com.sequoiacm.tools.common.SdbHelper;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ScmSiteInfo;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exec.ScmExecutorWrapper;
import com.sequoiadb.base.Sequoiadb;

public class ScmCreateNodeToolImpl extends ScmTool {
    private final String OPT_LONG_NAME = "name";
    private final String OPT_SHORT_NAME = "n";
    private final String OPT_LONG_SERVERURL = "serverurl";
    private final String OPT_SHORT_SERVERURL = "s";
    private final String OPT_LONG_SITENAME = "sitename";
    private final String OPT_SHORT_CUSTOM_PROP = "D";
    private final String OPT_SHORT_I = "I";
    private final String OPT_LONG_AUDIT_URL = "adurl";
    private final String OPT_LONG_AUDIT_USER = "aduser";
    private final String OPT_LONG_AUDIT_PASSED = "adpasswd";
    private final String OPT_LONG_GATEWAY = "gateway";
    private final String OPT_LONG_AMDIN_USER = "user";
    private final String OPT_LONG_AMDIN_PASSWORD = "passwd";
    private final String OPT_LONG_AMDIN_PASSWORD_FILE = "passwd-file";

    private Options ops;
    private String sysConfPath;
    private String log4jConfPath;
    private boolean isNeedRollBackDir = false;
    // private boolean isNeedRollBackRecord = false;
    private boolean isNeedRollBackSysConf = false;
    private boolean isNeedRollBackLog4jConf = false;
    private Map<String, String> customProp;
    private ScmHelpGenerator hp;
    private static final Logger logger = LoggerFactory
            .getLogger(ScmCreateNodeToolImpl.class.getName());
    private ScmSiteInfo mySiteInfo;
    private String auditUrl;
    private String auditUser = "sdbadmin";
    private String auditPassword = "sdbadmin";

    public ScmCreateNodeToolImpl() throws ScmToolsException {
        super("createnode");
        ops = new Options();
        hp = new ScmHelpGenerator();
        ops.addOption(
                hp.createOpt(OPT_SHORT_NAME, OPT_LONG_NAME, "new node name.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_SERVERURL, OPT_LONG_SERVERURL,
                "new node url(hostName:port).", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_SITENAME,
                "site name, new node belongs to this site.", true, true, false));
        ops.addOption(hp.createOpt(OPT_SHORT_CUSTOM_PROP, null,
                "custom node properties, eg:'-Dkey1=value1,-Dkey2=value2'.", false, true, false,
                true));
        ops.addOption(hp.createOpt(OPT_SHORT_I, null, "use current user.", false, false, true));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_AUDIT_URL, "audit to sdb url.", false, true, false));
        ops.addOption(
                hp.createOpt(null, OPT_LONG_AUDIT_USER, "audit to sdb user.", false, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AUDIT_PASSED, "audit to sdb passwd.", false, true,
                false));

        ops.addOption(hp.createOpt(null, OPT_LONG_GATEWAY,
                "gateway url, eg:'host1:port,host2:port,host3:port'.", true, true, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AMDIN_USER, "login admin username.", true, true,
                false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AMDIN_PASSWORD, "login admin password.",
                false, true, true, false, false));
        ops.addOption(hp.createOpt(null, OPT_LONG_AMDIN_PASSWORD_FILE, "login admin password file.",
                false, true, false));
        ScmContentCommandUtil.addDsOption(ops, hp);
    }

    @Override
    public void printHelp(boolean isHelpFull) {
        hp.printHelp(isHelpFull);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        CommandLine cl = ScmContentCommandUtil.parseArgs(args, ops);
        ScmSdbInfo mainSdb = ScmContentCommandUtil.parseDsOption(cl);
        String serverName = cl.getOptionValue(OPT_LONG_NAME);
        String mySiteName = cl.getOptionValue(OPT_LONG_SITENAME);
        String gatewayUrl = cl.getOptionValue(OPT_LONG_GATEWAY);
        ScmUserInfo adminUserInfo = ScmCommandUtil.checkAndGetUser(cl, OPT_LONG_AMDIN_USER,
                OPT_LONG_AMDIN_PASSWORD, OPT_LONG_AMDIN_PASSWORD_FILE);

        if (cl.hasOption(OPT_SHORT_CUSTOM_PROP)) {
            parseCustomProp(cl);
        }

        // int type = ScmCommon.convertStrToInt(cl.getOptionValue("type"));
        // if(type!=1){
        // throw new ScmToolsException("Unknow type:"+type,
        // ScmExitCode.INVALID_ARG);
        // }get hostname
        int type = 1;

        String url = cl.getOptionValue(OPT_LONG_SERVERURL);
        String[] hostAndPort = url.split(":");
        if (hostAndPort.length != 2) {
            throw new ScmToolsException("server'url must set as 'hostName:port',invalid url:" + url,
                    ScmExitCode.INVALID_ARG);
        }
        int port = ScmCommon.convertStrToInt(hostAndPort[1]);
        if (port > 65535 || port < 0) {
            throw new ScmToolsException("port out of range:" + port, ScmExitCode.INVALID_ARG);
        }
        String host = hostAndPort[0];
        if (!ScmCommon.isLocalHost(host)) {
            throw new ScmToolsException("hostName is not this machine:hostName=" + host,
                    ScmExitCode.INVALID_ARG);
        }
        ScmExecutorWrapper exe = new ScmExecutorWrapper();
        if (exe.getAllNode().containsKey(port)) {
            throw new ScmToolsException("The port is already occupied,port:" + port + ",conf path:"
                    + exe.getNodeConfPath(port), ScmExitCode.SCM_PORT_OCCUPIED);
        }

        if (cl.hasOption(OPT_LONG_AUDIT_URL)) {
            auditUrl = cl.getOptionValue(OPT_LONG_AUDIT_URL);
            auditUser = cl.getOptionValue(OPT_LONG_AUDIT_USER, auditUser);
            auditPassword = cl.getOptionValue(OPT_LONG_AUDIT_PASSED, auditPassword);
        }
        else {
            auditUrl = mainSdb.getSdbUrl();
            if (!Strings.isEmpty(mainSdb.getSdbPasswd())) {
                auditPassword = mainSdb.getSdbPasswd();
            }
            if (!Strings.isEmpty(mainSdb.getSdbUser())) {
                auditUser = mainSdb.getSdbUser();
            }
        }

        sysConfPath = ScmCommon.getScmConfAbsolutePath() + port + File.separator
                + ScmCommon.APPLICATION_PROPERTIES;
        log4jConfPath = ScmCommon.getScmConfAbsolutePath() + port + File.separator
                + ScmCommon.LOGCONF_NAME;
        Sequoiadb db = null;
        ScmSession ss = null;
        try {
            AuthInfo auth = ScmFilePasswordParser.parserFile(mainSdb.getSdbPasswd());
            db = SdbHelper.connectUrls(mainSdb.getSdbUrl(), mainSdb.getSdbUser(),
                    auth.getPassword());
            ScmMetaMgr mg = new ScmMetaMgr(db);
            mySiteInfo = mg.getSiteInfoByName(mySiteName);

            createConfFile(sysConfPath);
            isNeedRollBackSysConf = true;
            createConfFile(log4jConfPath);
            isNeedRollBackLog4jConf = true;

            createDefaultConf(mainSdb, port, sysConfPath, log4jConfPath);

            BSONObject serverRec = new BasicBSONObject();
            serverRec.put(FieldName.FIELD_CLCONTENTSERVER_NAME, serverName);
            // serverRec.put(FieldName.FIELD_CLCONTENTSERVER_ID, id);
            serverRec.put(FieldName.FIELD_CLCONTENTSERVER_TYPE, type);
            serverRec.put(FieldName.FIELD_CLCONTENTSERVER_SITEID, mySiteInfo.getId());
            serverRec.put(FieldName.FIELD_CLCONTENTSERVER_HOST_NAME, host);
            serverRec.put(FieldName.FIELD_CLCONTENTSERVER_PORT, port);

            ss = ScmFactory.Session.createSession(
                    new ScmConfigOption(ScmContentCommandUtil.parseListUrls(gatewayUrl),
                            adminUserInfo.getUsername(), adminUserInfo.getPassword()));
            RestDispatcher.getInstance().createNode(ss, serverRec);
        }
        catch (ScmToolsException e) {
            rollBackFile();
            logger.info("Failed to create node,error:{}", e.getMessage(), e);
            throw e;
        }
        catch (Exception e) {
            rollBackFile();
            logger.info("Failed to create node,error:{}", e.getMessage(), e);
            throw new ScmToolsException("Failed to create node,error:" + e.getMessage(),
                    ScmExitCode.SYSTEM_ERROR, e);
        }
        finally {
            ScmCommon.closeResource(ss);
            SdbHelper.closeCursorAndDb(db);
        }
        System.out.println("Create node success:" + serverName);
    }

    private void parseCustomProp(CommandLine cl) throws ScmToolsException {
        Properties prop = cl.getOptionProperties(OPT_SHORT_CUSTOM_PROP);
        customProp = new HashMap<String, String>();
        for (Entry<Object, Object> entry : prop.entrySet()) {
            customProp.put(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    private void rollBackFile() {
        File sysConfFile = new File(sysConfPath);
        File log4jConfFile = new File(log4jConfPath);
        if (isNeedRollBackSysConf) {
            try {
                if (sysConfFile.exists()) {
                    sysConfFile.delete();
                }
            }
            catch (Exception e1) {
                System.out.println("Failed to rollback,failed to delete file:" + sysConfPath
                        + ",errorMsg:" + e1.getMessage());
            }
        }
        if (isNeedRollBackLog4jConf) {
            try {
                if (log4jConfFile.exists()) {
                    log4jConfFile.delete();
                }
            }
            catch (Exception e2) {
                System.out.println("Failed to rollback,failed to delete file:" + log4jConfPath
                        + ",errorMsg:" + e2.getMessage());
            }
        }
        if (isNeedRollBackDir) {
            try {
                if (sysConfFile.getParentFile().exists()) {
                    sysConfFile.getParentFile().delete();
                }
            }
            catch (Exception e3) {
                System.out.println("Failed to rollback,failed to delete dir:"
                        + sysConfFile.getParentFile().getPath() + ",errorMsg:" + e3.getMessage());
            }
        }
    }

    private void createConfFile(String filePath) throws ScmToolsException {
        File file = new File(filePath);
        if (ScmCommon.isFileExists(filePath)) {
            throw new ScmToolsException("Failed to create " + file.getName() + "," + file.getName()
                    + " already exist:" + file.toString(), ScmExitCode.FILE_ALREADY_EXIST);
        }
        if (!ScmCommon.isFileExists(file.getParentFile().getPath())) {
            isNeedRollBackDir = true;
        }
        ScmCommon.createFile(filePath);
    }

    private void createDefaultConf(ScmSdbInfo mainSite, int port, String sysConfPath,
            String log4jConfPath) throws ScmToolsException {

        Map<String, String> modifier = new HashMap<>();

        // sysconf.properties
        modifier.put(PropertiesDefine.PROPERTY_SERVER_PORT, port + "");
        modifier.put(PropertiesDefine.PROPERTY_ROOTSITE_URL, mainSite.getSdbUrl());
        modifier.put(PropertiesDefine.PROPERTY_ROOTSITE_USER, mainSite.getSdbUser());
        modifier.put(PropertiesDefine.PROPERTY_ROOTSITE_PASSWD, mainSite.getSdbPasswd());
        modifier.put(PropertiesDefine.PROPERTY_SCM_SPRING_APP_NAME, mySiteInfo.getName());
        modifier.put(PropertiesDefine.PROPERTY_SCM_EUREKA_METADATA_IS_ROOTSITE,
                mySiteInfo.isRootSite() + "");
        modifier.put(PropertiesDefine.PROPERTY_SCM_EUREKA_METADATA_SITE_ID,
                mySiteInfo.getId() + "");

        // put custom props
        if (customProp != null) {
            modifier.putAll(customProp);
        }

        // save
        writeDefaultConf(ScmCommon.SCM_SAMPLE_SYS_CONF_NAME, sysConfPath, modifier);

        // clear
        modifier.clear();

        // log4j.properties
        String logOutputPath = ".." + File.separator + "log" + File.separator
                + ScmCommon.SCM_LOG_DIR_NAME + File.separator + port;
        modifier.put(PropertiesUtil.SAMPLE_VALUE_SCM_LOG_PATH, logOutputPath);
        modifier.put(PropertiesUtil.SAMPLE_VALUE_SCM_AUDIT_SDB_URL, auditUrl);
        modifier.put(PropertiesUtil.SAMPLE_VALUE_SCM_AUDIT_SDB_USER, auditUser);
        modifier.put(PropertiesUtil.SAMPLE_VALUE_SCM_AUDIT_SDB_PASSWD, auditPassword);
        logger.info("auditUrl=" + auditUrl + ", auditUser=" + auditUser + ", auditPasswd= "
                + auditPassword);
        // save
        writeDefaultConf(ScmCommon.SCM_SAMPLE_LOG_CONF_NAME, log4jConfPath, modifier);
    }

    private void writeDefaultConf(String sampleResFile, String outputPath,
            Map<String, String> modifier) throws ScmToolsException {
        InputStream is = ScmCtl.class.getClassLoader().getResourceAsStream(sampleResFile);
        if (is == null) {
            logger.error("missing resource file:" + sampleResFile);
            throw new ScmToolsException("missing resource file:" + sampleResFile,
                    ScmExitCode.SYSTEM_ERROR);
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            bw = new BufferedWriter(new FileWriter(outputPath));
            if (sampleResFile.equals(ScmCommon.SCM_SAMPLE_SYS_CONF_NAME)) {
                modifyForAppConf(modifier, br, bw);
            }
            else {
                modifyForLogConf(modifier, br, bw);
            }

        }
        catch (IOException e) {
            logger.error("write config to " + outputPath + " occur error", e);
            throw new ScmToolsException(
                    "write config to " + outputPath + " occur error:" + e.getMessage(),
                    ScmExitCode.IO_ERROR);
        }
        finally {
            close(bw);
            close(br);
        }
    }

    private void modifyForAppConf(Map<String, String> modifier, BufferedReader br,
            BufferedWriter bw) throws IOException {
        String line = null;
        while ((line = br.readLine()) != null) {
            Iterator<Entry<String, String>> modifierIt = modifier.entrySet().iterator();
            while (modifierIt.hasNext()) {
                String modifyKey = modifierIt.next().getKey();
                if (line.contains(modifyKey)) {
                    String[] arr = line.split("=");
                    if (arr[0].trim().equals(modifyKey)) {
                        line = modifyKey + "=" + modifier.get(modifyKey);
                        modifierIt.remove();
                        break;
                    }
                }
            }
            bw.write(line);
            bw.newLine();
        }

        if (!modifier.isEmpty()) {
            bw.newLine();
            bw.write("#custom properties");
            bw.newLine();
            for (Entry<String, String> prop : modifier.entrySet()) {
                bw.write(prop.getKey() + "=" + prop.getValue());
                bw.newLine();
            }
        }
    }

    private void modifyForLogConf(Map<String, String> modifier, BufferedReader br,
            BufferedWriter bw) throws IOException {
        String line = null;
        while ((line = br.readLine()) != null) {
            for (Entry<String, String> entry : modifier.entrySet()) {
                if (line.contains(entry.getKey())) {
                    line = line.replace(entry.getKey(), entry.getValue());
                    break;
                }
            }
            bw.write(line);
            bw.newLine();
        }
    }

    private void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        }
        catch (Exception e) {
            logger.warn("close file occur error", e);
        }
    }
}
