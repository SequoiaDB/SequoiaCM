package com.sequoiacm.mappingutil.element;

import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.common.CommonDefine;
import com.sequoiacm.mappingutil.common.CommonUtils;
import org.apache.commons.cli.CommandLine;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.sequoiacm.mappingutil.command.ScmFileMappingToolImpl.*;

public class MappingOption {

    private String workPath;
    private String workspace;
    private String bucket;
    private BSONObject fileMatcher;
    private String idFilePath;
    private ScmBucketAttachKeyType keyType;
    private List<String> urlList;
    private ScmUserInfo userInfo;

    private long maxFails = 100;
    private int batchSize = 5000;
    private int attachSize = 50;
    private int thread = 20;

    public MappingOption(CommandLine cl) throws ScmToolsException {
        this.workPath = getAbsolutePath(cl.getOptionValue(OPT_LONG_WORK_PATH));
        this.workspace = cl.getOptionValue(OPT_LONG_WORKSPACE);
        this.bucket = cl.getOptionValue(OPT_LONG_BUCKET);
        this.keyType = ScmBucketAttachKeyType.valueOf(cl.getOptionValue(OPT_LONG_KEY_TYPE));

        if (cl.hasOption(OPT_LONG_FILE_MATCHER)) {
            CommonUtils.assertTrue(!cl.hasOption(OPT_LONG_FILE_ID),
                    "do not specify --" + OPT_LONG_FILE_ID + " and " + "--" + OPT_LONG_FILE_MATCHER
                            + " at the same time");
            String fileMatcherStr = cl.getOptionValue(OPT_LONG_FILE_MATCHER);
            this.fileMatcher = fileMatcherStr.equals(CommonDefine.FILE_MATCHER_ALL)
                    ? new BasicBSONObject()
                    : (BSONObject) JSON.parse(fileMatcherStr);
        }
        else {
            this.idFilePath = cl.getOptionValue(OPT_LONG_FILE_ID);
            CommonUtils.assertTrue(idFilePath != null, "please specify one of --"
                    + OPT_LONG_FILE_ID + "and --" + OPT_LONG_FILE_MATCHER);
            this.idFilePath = getAbsolutePath(idFilePath);
            CommonUtils.assertTrue(new File(idFilePath).exists(),
                    "Id file not exist, path=" + idFilePath);
        }

        this.urlList = Arrays.asList(cl.getOptionValue(OPT_LONG_URL).split(","));
        this.userInfo = ScmCommandUtil.checkAndGetUser(cl, OPT_LONG_USER, OPT_LONG_PASSWORD,
                OPT_LONG_PASSWORD_FILE);

        if (cl.hasOption(OPT_LONG_MAX_FAILS)) {
            this.maxFails = Long.parseLong(cl.getOptionValue(OPT_LONG_MAX_FAILS));
            CommonUtils.assertTrue(maxFails > 0,
                    OPT_LONG_MAX_FAILS + " must be greater than or equals to 0: " + maxFails);
        }
        if (cl.hasOption(OPT_LONG_BATCH_SIZE)) {
            this.batchSize = Integer.parseInt(cl.getOptionValue(OPT_LONG_BATCH_SIZE));
            CommonUtils.assertTrue(batchSize > 0 && batchSize <= 10000, OPT_LONG_BATCH_SIZE
                    + " must be greater than or equals to 1 and less than or equal to 10000: "
                    + batchSize);
        }
        if (cl.hasOption(OPT_LONG_ATTACH_SIZE)) {
            this.attachSize = Integer.parseInt(cl.getOptionValue(OPT_LONG_ATTACH_SIZE));
            CommonUtils.assertTrue(attachSize > 0 && attachSize <= 100,
                    OPT_LONG_ATTACH_SIZE
                            + " must be greater than or equals to 1 and less than or equal to 100: "
                            + attachSize);
        }
        if (cl.hasOption(OPT_LONG_THREAD)) {
            thread = Integer.parseInt(cl.getOptionValue(OPT_LONG_THREAD));
            CommonUtils.assertTrue(thread > 0 && thread <= 100,
                    OPT_LONG_THREAD
                            + " must be greater than or equals to 1 and less than or equal to 100: "
                            + thread);
        }
    }

    private String getAbsolutePath(String path) throws ScmToolsException {
        File file = new File(path);
        if (file.isAbsolute()) {
            return path;
        }
        String userWorkingDir = ScmCommon.getUserWorkingDir();
        return CommonUtils.getStandardDirPath(userWorkingDir) + path;
    }

    public String getWorkPath() {
        return workPath;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getBucket() {
        return bucket;
    }

    public BSONObject getFileMatcher() {
        return fileMatcher;
    }

    public String getIdFilePath() {
        return idFilePath;
    }

    public ScmBucketAttachKeyType getKeyType() {
        return keyType;
    }

    public List<String> getUrlList() {
        return urlList;
    }

    public ScmUserInfo getUserInfo() {
        return userInfo;
    }

    public long getMaxFails() {
        return maxFails;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getAttachSize() {
        return attachSize;
    }

    public int getThread() {
        return thread;
    }
}
