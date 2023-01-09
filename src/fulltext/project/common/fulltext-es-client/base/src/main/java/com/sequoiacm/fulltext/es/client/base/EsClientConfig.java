package com.sequoiacm.fulltext.es.client.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@ConfigurationProperties("scm.fulltext.es")
public class EsClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(EsClientConfig.class);
    private List<String> urls = Arrays.asList("http://localhost:9200");
    private int searchScrollTimeout = 60 * 1000 * 3;

    private int searchScrollSize = 100;
    private int indexShards = 5;
    private int indexReplicas = 1;
    private SyncRefreshPolicy syncRefreshPolicy = SyncRefreshPolicy.WAIT_UNTIL;
    private String analyzer = "ik_max_word";
    private String searchAnalyzer = "ik_smart";
    private String user;
    private String password;
    private String certPath;
    private String adapterPath;

    public String getAdapterPath() {
        return adapterPath;
    }

    public void setAdapterPath(String adapterPath) throws FullTextException {
        this.adapterPath = adapterPath;
    }

    @PostConstruct
    private void checkAdapterPath() throws FullTextException {
        if (adapterPath == null || adapterPath.trim().length() == 0) {
            adapterPath = getDefaultAdapterPath();
        }
    }

    private String getDefaultAdapterPath() throws FullTextException {
        File jarsDir = new File("." + File.separator + "jars");
        List<String> adapterDirs = new ArrayList<>();
        for (File subFile : jarsDir.listFiles()) {
            if (subFile.isDirectory() && subFile.getName().startsWith("es-client")) {
                adapterDirs.add(subFile.getAbsolutePath());
            }
        }
        if (adapterDirs.isEmpty()) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "es adapter not found");
        }
        Collections.sort(adapterDirs);
        String defaultAdapter = adapterDirs.get(adapterDirs.size() - 1);
        logger.info("es adapter list: {}, use {}", adapterDirs, defaultAdapter);
        return defaultAdapter;
    }

    public SyncRefreshPolicy getSyncRefreshPolicy() {
        return syncRefreshPolicy;
    }

    public void setSyncRefreshPolicy(SyncRefreshPolicy syncRefreshPolicy) {
        this.syncRefreshPolicy = syncRefreshPolicy;
    }

    public int getSearchScrollSize() {
        return searchScrollSize;
    }

    public int getSearchScrollTimeout() {
        return searchScrollTimeout;
    }

    public void setSearchScrollSize(int searchScrollSize) {
        this.searchScrollSize = searchScrollSize;
    }

    public void setSearchScrollTimeout(int searchScrollTimeout) {
        this.searchScrollTimeout = searchScrollTimeout;
    }

    public int getIndexReplicas() {
        return indexReplicas;
    }

    public int getIndexShards() {
        return indexShards;
    }

    public void setIndexReplicas(int indexReplicas) {
        this.indexReplicas = indexReplicas;
    }

    public void setIndexShards(int indexShards) {
        this.indexShards = indexShards;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public String getSearchAnalyzer() {
        return searchAnalyzer;
    }

    public void setSearchAnalyzer(String searchAnalyzer) {
        this.searchAnalyzer = searchAnalyzer;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String passwordFile) {
        if (passwordFile != null && passwordFile.trim().length() > 0) {
            AuthInfo auth = ScmFilePasswordParser.parserFile(passwordFile);
            this.password = auth.getPassword();
        }
        else {
            this.password = null;
        }
    }
}
