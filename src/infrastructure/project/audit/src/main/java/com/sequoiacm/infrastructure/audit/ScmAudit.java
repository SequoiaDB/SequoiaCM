package com.sequoiacm.infrastructure.audit;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.sequoiacm.infrastructrue.security.core.ScmUser;

@RefreshScope
@Component
public class ScmAudit {
    private static final Logger logger = LoggerFactory.getLogger(ScmAudit.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    // key is username and value is audit type mask
    private Map<String, Integer> auditUser = null;

    // key is user type and value is audit type mask
    private Map<String, Integer> auditUserType = null;

    @Value("${server.port}")
    private int myPort;

    private String myHostName;

    @Autowired
    public ScmAudit(ScmAuditConfig auditConfig) throws UnknownHostException {
        init(auditConfig);
        myHostName = InetAddress.getLocalHost().getHostName();
    }

    public void info(ScmAuditType auditType, Authentication auth, String wsName,
            int flag, String message) {
        ScmUserAuditType  userAuditType = ScmUserAuditType.SYSTEM_USER;
        String userName = ScmUserAuditType.SYSTEM_USER.getName();
        if (auth != null) {
            ScmUser scmUser = (ScmUser) auth.getPrincipal();
            String passwordType = scmUser.getPasswordType().toString();
            userAuditType = ScmUserAuditType.getScmUserAuditType(passwordType);
            Assert.notNull(userAuditType, "unknown password type:" + passwordType);
            userName = scmUser.getUsername();
        }

        info(auditType, userAuditType, userName, wsName, flag, message);
    }

    public void info(ScmAuditType auditType, ScmUserAuditType userAuditType, String userName,
            String wsName, int flag, Object message) {
        Integer opAuditTypeValue = auditType.getType();
        Integer userAuditTypeValue = auditUser.get(userName);
        // user configuration priority is gereater than user type
        if (userAuditTypeValue != null) {
            if ((userAuditTypeValue & opAuditTypeValue) != 0) {
                auditLogger.info("host:" + myHostName + ",port:" + myPort + ",type:"
                        + auditType.getName() + ",userType:" + userAuditType.getName() + ",user:"
                        + userName + ",ws:" + wsName + ",flag:" + flag + "," + message);
            }
            return;
        }

        // User type of self
        String userType = userAuditType.getName();
        Integer userClassAuditTypeValue = auditUserType.get(userType);
        if (userClassAuditTypeValue != null) {
            if ((userClassAuditTypeValue & opAuditTypeValue) != 0) {
                auditLogger.info("host:" + myHostName + ",port:" + myPort + ",type:"
                        + auditType.getName() + ",userType:" + userAuditType.getName() + ",user:"
                        + userName + ",ws:" + wsName + ",flag:" + flag + "," + message);
            }
            return;
        }
        // User type of ALL
        Integer allUserClassAuditTypeValue = auditUserType.get(ScmUserAuditType.ALL_USER.getName());
        if (allUserClassAuditTypeValue != null) {
            if ((allUserClassAuditTypeValue & opAuditTypeValue) != 0) {
                auditLogger.info("host:" + myHostName + ",port:" + myPort + ",type:"
                        + auditType.getName() + ",userType:" + userAuditType.getName() + ",user:"
                        + userName + ",ws:" + wsName + ",flag:" + flag + "," + message);
            }
        }

    }

    public void init(ScmAuditConfig auditConfig) {
        Map<String, String> userMap = auditConfig.getUser();
        Map<String, String> userTypeMap = maskAddUserClass(auditConfig);
        auditUser = new HashMap<String, Integer>();
        auditUserType = new HashMap<String, Integer>();

        Set<Entry<String, String>> userEntrySet = userMap.entrySet();
        for (Entry<String, String> userEntry : userEntrySet) {
            Integer auditTypeMaskValue = parseAduitType(userEntry.getValue());
            auditUser.put(userEntry.getKey(), auditTypeMaskValue);
        }
        Set<Entry<String, String>> userTypeEntrySet = userTypeMap.entrySet();
        for (Entry<String, String> userTypeEntry : userTypeEntrySet) {
            Integer auditTypeMaskValue = parseAduitType(userTypeEntry.getValue());
            auditUserType.put(userTypeEntry.getKey(), auditTypeMaskValue);
        }

        logger.info("user audit configuration set = " + userMap.toString());
        logger.info("user type audit configuration set = " + userTypeMap.toString());
    }

    // if userType and userMask is no conflict,
    // then mask=ALL usermask=LOCAL -->userType.LOCAL=ALL
    private Map<String, String> maskAddUserClass(ScmAuditConfig auditConfig) {
        String mask = auditConfig.getMask();
        String usermask = auditConfig.getUserMask();
        Map<String, String> newUserTypeMap = new HashMap<String, String>();
        newUserTypeMap.putAll(auditConfig.getUserType());
        if (StringUtils.isEmpty(usermask)) {
            return newUserTypeMap;
        }
        String[] userTypeSet = usermask.split("\\|");
        for (String userType : userTypeSet) {
            if (!newUserTypeMap.containsKey(userType)) {
                newUserTypeMap.put(userType, mask);
            }
        }
        return newUserTypeMap;
    }

    // parse : ALL|WS_DQL --> 0xFFFFFF00
    private int parseAduitType(String auditType) {
        int aduitTypeMaskValue = 0;
        String[] flags = auditType.split("\\|");
        for (int i = 0; i < flags.length; i++) {
            ScmAuditType type = ScmAuditType.getScmAuditType(flags[i]);
            if (type != null && type.isTopLevel()) {
                aduitTypeMaskValue |= type.getType();
            }
        }
        return aduitTypeMaskValue;
    }
}
