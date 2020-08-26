package com.sequoiacm.fulltext.server.es;

import java.util.Arrays;
import java.util.List;

import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("scm.fulltext.es")
@Component
public class EsClientConfig {
    private List<String> urls = Arrays.asList("http://192.168.20.46:9200");
    private int searchScrollTimeout = 60 * 1000 * 3;
    
    private int searchScrollSize = 1000;
    private int indexShards = 5;
    private int indexReplicas = 1;
    private RefreshPolicy syncRefreshPolicy = RefreshPolicy.WAIT_UNTIL;

    public RefreshPolicy getSyncRefreshPolicy() {
        return syncRefreshPolicy;
    }

    public void setSyncRefreshPolicy(RefreshPolicy syncRefreshPolicy) {
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

}
