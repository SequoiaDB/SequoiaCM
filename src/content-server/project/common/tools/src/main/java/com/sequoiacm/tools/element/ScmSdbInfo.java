package com.sequoiacm.tools.element;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmSdbInfo {
    private final Logger logger = LoggerFactory.getLogger(ScmSdbInfo.class);

    private String sdbUrl;
    private String sdbUser;
    private String sdbPasswdFile;

    public ScmSdbInfo() {
    }

    public ScmSdbInfo(String sdbUrl, String sdbUser, String sdbPasswdFile) throws ScmToolsException {
        this.sdbPasswdFile = sdbPasswdFile;
        this.sdbUrl = sdbUrl;
        this.sdbUser = sdbUser;
    }

    public String getSdbUrl() {
        return sdbUrl;
    }

    public List<String> getSdbUrlList() {
        List<String> retList = new ArrayList<>();
        String[] urlArr = sdbUrl.split(",");
        for (String url : urlArr) {
            retList.add(url);
        }
        return retList;
    }

    public void setSdbUrl(String sdbUrl) {
        this.sdbUrl = sdbUrl;
    }

    public String getSdbUser() {
        return sdbUser;
    }

    public void setSdbUser(String sdbUser) {
        this.sdbUser = sdbUser;
    }

    public String getSdbPasswdFile() {
        return sdbPasswdFile;
    }

    public String getPlainSdbPasswd() {
        return ScmFilePasswordParser.parserFile(sdbPasswdFile).getPassword();
    }

    public void setSdbPasswdFile(String sdbPasswdFile) {
        this.sdbPasswdFile = sdbPasswdFile;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        ScmSdbInfo tmp = (ScmSdbInfo) obj;
        if (tmp.getSdbPasswdFile().equals(sdbPasswdFile) && tmp.getSdbUrl().equals(sdbUrl)
                && tmp.getSdbUser().equals(sdbUser)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ScmSdbInfo [sdbUrl=" + sdbUrl + ", sdbUser=" + sdbUser + ", sdbPasswd=" + sdbPasswdFile
                + "]";
    }

}