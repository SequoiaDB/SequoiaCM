package com.sequoiacm.config.framework.site.tool;

import com.sequoiacm.common.CephS3UrlInfo;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScmSiteTool {

    public static boolean checkSiteConfigValid(List<Config> allSiteConfigs, SiteConfig siteConfig) throws ScmConfigException {
        Set<String> allSiteUrlSet = ScmSiteTool.siteListUrlToSet(allSiteConfigs);
        BSONObject newSiteDataSource = siteConfig.getDataSource();
        List<String> newSiteUrlList = ScmSiteTool.siteDSUrlToList(newSiteDataSource);

        for (String url : newSiteUrlList){
            if (allSiteUrlSet.contains(url)){
                throw new ScmConfigException(ScmConfError.DATASOURCE_EXIST_OTHER_SITE,
                        "datasource already exist in other site, datasource =" + url);
            }
        }
        return true;
    }

    public static Set<String> siteListUrlToSet(List<Config> allSites) {
        Set<String> allUrlSet = new HashSet<>();
        for (Config config : allSites){
            SiteConfig siteConfig = (SiteConfig) config;
            BSONObject dataSource = siteConfig.getDataSource();
            List<String> list = siteDSUrlToList(dataSource);
            allUrlSet.addAll(list);
        }
        return allUrlSet;
    }

    public static List<String> siteDSUrlToList(BSONObject datasource) {
        List<String> siteUrlList = new ArrayList<>();
        String datasourceType = (String) datasource.get(FieldName.FIELD_CLSITE_DATA_TYPE);
        BasicBSONList urlStrList = (BasicBSONList) datasource.get(FieldName.FIELD_CLSITE_URL);
        if (datasourceType.equals(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR)){
            for (Object o : urlStrList){
                String url = (String) o;
                CephS3UrlInfo cephS3UrlInfo = new CephS3UrlInfo(url);
                siteUrlList.add(cephS3UrlInfo.getUrl());
            }
        } else {
            List<String> list = (List<String>) urlStrList.asList();
            siteUrlList.addAll(list);
        }
        return siteUrlList;
    }
}
