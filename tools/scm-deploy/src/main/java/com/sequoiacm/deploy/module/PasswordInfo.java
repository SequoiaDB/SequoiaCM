package com.sequoiacm.deploy.module;

import java.io.File;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiadb.util.SdbDecrypt;
import com.sequoiadb.util.SdbDecryptUserInfo;

public class PasswordInfo {
    // private static final Logger logger =
    // LoggerFactory.getLogger(PasswordInfo.class);
    private String plaintext;
    private String filePath;

    public PasswordInfo(DatasourceType type, String userName, String password,
            String passwordFile) {
        if (passwordFile == null || passwordFile.length() == 0) {
            plaintext = password;
            if (plaintext == null) {
                plaintext = "";
            }
            return;
        }

        if (!new File(passwordFile).exists()) {
            throw new IllegalArgumentException("password file not exist:" + passwordFile);
        }

        if (type == DatasourceType.SEQUOIADB) {
            parseSdbPwdFile(userName, password, passwordFile);
        }
        else {
            filePath = passwordFile;
        }
    }

    private void parseSdbPwdFile(String userName, String token, String passwordFile) {
        try {
            SdbDecrypt sdbDec = new SdbDecrypt();
            SdbDecryptUserInfo info = sdbDec.parseCipherFile(userName, token,
                    new File(passwordFile));
            plaintext = info.getPasswd();
        }
        catch (Exception e) {
            throw new IllegalArgumentException("failed to decrypt password file:" + passwordFile,
                    e);
        }
    }


    public String getPlaintext() {
        return plaintext;
    }

    public String getFilePath() {
        return filePath;
    }

}
