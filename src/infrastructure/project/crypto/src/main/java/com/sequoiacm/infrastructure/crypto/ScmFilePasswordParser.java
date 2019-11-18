package com.sequoiacm.infrastructure.crypto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmFilePasswordParser {
    private static final Logger logger = LoggerFactory.getLogger(ScmFilePasswordParser.class);

    public static AuthInfo parserFile(String passwdFile) {
        if (null == passwdFile || passwdFile.length() == 0) {
            return new AuthInfo();
        }

        return parserFile(new File(passwdFile));
    }

    private static AuthInfo parserFile(File f) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String value = br.readLine();
            value = value.trim();

            int sepIndex = value.lastIndexOf(":");
            if (sepIndex == -1 || sepIndex >= value.length() - 1) {
                throw new RuntimeException("parse content failed:content=" + value);
            }

            String userName = value.substring(0, sepIndex);
            String encryptedPassword = value.substring(sepIndex + 1, value.length());

            String password = ScmPasswordMgr.getInstance()
                    .decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES, encryptedPassword);

            AuthInfo auth = new AuthInfo();
            auth.setUserName(userName);
            auth.setPassword(password);
            return auth;
        }
        catch (Exception e) {
            logger.warn("parse password file failed:file={}", f, e);
            return new AuthInfo();
        }
        finally {
            if (null != br) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    logger.warn("close br failed:file={}", f, e);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // 5CC511588BA9F4551F4F 123
        AuthInfo info = ScmFilePasswordParser.parserFile(new File((String) null));
        System.out.println(info);

        info = ScmFilePasswordParser.parserFile(new File("d:\\afa.txt"));
        System.out.println(info);
    }
}
