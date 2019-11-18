package com.sequoiacm.infrastructure.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmManifestParser {
    public static final Logger logger = LoggerFactory.getLogger(ScmManifestParser.class);
    public static final String Build_Time = "Build-Time";
    public static final String SCM_Version = "SCM-Version";
    public static final String SCM_Git_Commit_Id = "SCM-Git-Commit-Id";
    public static final String SCM_Git_Path = "SCM-Git-Path";
    public static final String SCM_Git_Branch = "SCM-Git-Branch";
    public static final String SCM_Svn_Revision = "SCM-Svn-Revision";
    public static final String SCM_Svn_CommittedRevision = "SCM-Svn-CommittedRevision";
    public static final String SCM_Svn_Path = "SCM-Svn-Path";
    public static final String SCM_Svn_MixedRevisions = "SCM-Svn-MixedRevisions";

    public static class ManifestInfo {
        private String scmVersion;
        private String buildTime;
        private String scmGitCommitId;
        private String scmSvnRevision;

        public ManifestInfo(Properties prop) {
            if (prop == null) {
                return;
            }
            scmVersion = prop.getProperty(SCM_Version);
            if (scmVersion != null && scmVersion.trim().length() == 0) {
                scmVersion = null;
            }

            buildTime = prop.getProperty(Build_Time);
            if (buildTime != null && buildTime.trim().length() == 0) {
                buildTime = null;
            }

            scmGitCommitId = prop.getProperty(SCM_Git_Commit_Id);
            if (scmGitCommitId != null && scmGitCommitId.trim().length() == 0) {
                scmGitCommitId = null;
            }

            scmSvnRevision = prop.getProperty(SCM_Svn_Revision);
            if (scmSvnRevision != null && (scmSvnRevision.trim().length() == 0
                    || scmSvnRevision.trim().equals("-1"))) {
                scmSvnRevision = null;
            }
        }

        public String getScmVersion() {
            return scmVersion;
        }

        public String getBuildTime() {
            return buildTime;
        }

        public String getScmGitCommitId() {
            return scmGitCommitId;
        }

        public String getScmSvnRevision() {
            return scmSvnRevision;
        }

        public String getGitCommitIdOrSvnRevision() {
            if (scmGitCommitId != null) {
                return scmGitCommitId;
            }

            if (scmSvnRevision != null) {
                return scmSvnRevision;
            }
            return null;
        }
    }

    public static ManifestInfo getManifestInfoFromJar(Class<?> classInTheJar) throws IOException {
        String className = classInTheJar.getSimpleName() + ".class";
        String classPath = classInTheJar.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            // when ide start the app.
            logger.warn("class not from jar,loadManifest do noting:class={}", classPath);
            return new ManifestInfo(null);
        }

        String manifestPath = classPath.substring(0, classPath.indexOf("!") + 1)
                + "/META-INF/MANIFEST.MF";
        URL manifestUrl = new URL(manifestPath);
        logger.info("load version info from manifest:url={}", manifestUrl.toString());

        InputStream is = null;
        try {
            is = manifestUrl.openStream();
            Manifest manifest = new Manifest(is);
            Attributes a = manifest.getMainAttributes();
            Properties prop = new Properties();
            for (Entry<Object, Object> set : a.entrySet()) {
                prop.put(set.getKey().toString(), set.getValue().toString());
            }
            return new ManifestInfo(prop);
        }
        finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            catch (Exception e) {
                logger.warn("failed to close inputstream:url={}", manifestUrl, e);
            }
        }
    }
}
