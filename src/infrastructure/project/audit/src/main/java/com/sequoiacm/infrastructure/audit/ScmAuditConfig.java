package com.sequoiacm.infrastructure.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;



@Component
@RefreshScope
@ConfigurationProperties(prefix = "scm.audit")
public class ScmAuditConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuditConfig.class);

    private String mask = "";

    private String userMask = "";

    // key is username and value is audit type
    private Map<String, String> user = new HashMap<String, String>();

    // key is user type and value is audit type
    private Map<String, String> userType = new HashMap<String, String>();
    
    // user and userClass formating
    @PostConstruct
    private void init() {
        if (!user.isEmpty()) {
            HashMap<String, String> userMap=new HashMap<String,String>();
            Set<Entry<String, String>> userEntrySet = user.entrySet();
            for (Entry<String, String> userEntry : userEntrySet) {
                String username= userEntry.getKey();
                String auditType = userEntry.getValue();
                String usernameTrim=username.trim();
                if (StringUtils.isEmpty(usernameTrim)) {
                    logger.warn("Invalid audit username: " + username + ", audit type:" + auditType);
                    continue;
                }
                auditType = formatAuditType(auditType);
                userMap.put(usernameTrim, auditType);
            }
            this.user=userMap;
        }
        
        if (!userType.isEmpty()) {
            HashMap<String, String> userTypeMap=new HashMap<String,String>();
            Set<Entry<String, String>> userTypeEntrySet = userType.entrySet();
            for (Entry<String, String> userTypeEntry : userTypeEntrySet) {
                String userAuditType = userTypeEntry.getKey();
                String auditType = userTypeEntry.getValue();
                String userAuditTypeTrim=userAuditType.trim();
                ScmUserAuditType type=ScmUserAuditType.getScmUserAuditType(userAuditTypeTrim);
                if (type == null) {
                    logger.warn("Invalid audit userType: " + userAuditType + ", audit type:" + auditType);
                    continue;
                }
                auditType = formatAuditType(auditType);
                userTypeMap.put(userAuditTypeTrim, auditType);
            }
            this.userType=userTypeMap;
        }
    }
    
    public String getMask() {
        return mask;
    }

    public String getUserMask() {
        return userMask;
    }

    public Map<String, String> getUser() {
        return user;
    }

    public Map<String, String> getUserType() {
        return userType;
    }

    
    public void setMask(String mask) {
        if (StringUtils.isEmpty(mask)) {
            return;
        }
        this.mask = formatAuditType(mask);
    }

    public void setUserMask(String userMask) {
        if (StringUtils.isEmpty(userMask)) {
            return;
        }
        this.userMask = formatUserAuditType(userMask);
        
    }

    public void setUser(Map<String, String> user) {
        this.user = user;
    }

    public void setUserType(Map<String, String> userType) {
        this.userType = userType;
    }

    private String formatAuditType(String aduitType){
        StringBuilder sb = new StringBuilder();
        String[] aduitTypes = aduitType.split("\\|");
        for (int i = 0; i < aduitTypes.length; i++) {
            String tmp = aduitTypes[i].trim();
            ScmAuditType type = ScmAuditType.getScmAuditType(tmp);
            if (type!=null) {
                sb.append(tmp).append("|");
            }
            else {
                logger.warn("Invalid audit type: " + tmp);
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    private String formatUserAuditType(String userAuditType){
        StringBuilder sb = new StringBuilder();
        String[] userAuditTypes = userAuditType.split("\\|");
        for (int i = 0; i < userAuditTypes.length; i++) {
            String tmp = userAuditTypes[i].trim();
            ScmUserAuditType type = ScmUserAuditType.getScmUserAuditType(tmp);
            if (type!=null) {
                sb.append(tmp).append("|");
            }
            else {
                logger.warn("Invalid user audit type: " + tmp);
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}