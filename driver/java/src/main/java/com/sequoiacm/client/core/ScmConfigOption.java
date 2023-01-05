package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;

/**
 * Option for config session.
 *
 * @since 2.1
 */
public class ScmConfigOption {
    static final String DEFAULT_REGION = "DefaultRegion";
    static final String DEFAULT_ZONE = "zone1";

    private String region = DEFAULT_REGION;
    private String zone = DEFAULT_ZONE;
    private ScmUrlConfig urlConfig;

    private String user;
    private String passwd;

    private ScmRequestConfig requestConfig;

    /**
     * Create new instance by url,region,zone,user,passwd,and requestConfig.
     *
     * @param url
     *            service's url, the url's format is host:port .
     * @param region
     *            service's region
     * @param zone
     *            service's zone
     * @param user
     *            sequoiaCM user name.
     * @param passwd
     *            sequoiaCM password.
     * @param requestConfig
     *            request's configure
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public ScmConfigOption(String url, String region, String zone, String user, String passwd,
            ScmRequestConfig requestConfig) throws ScmException {
        if (null == url) {
            throw new ScmInvalidArgumentException("url is null");
        }

        List<String> urlList = new ArrayList<String>();
        urlList.add(transformUrl(url));
        this.urlConfig = generateUrlConfig(region, zone, urlList);
        this.region = region;
        this.zone = zone;

        this.user = user;
        this.passwd = passwd;
        this.requestConfig = requestConfig;
    }

    /**
     * Create new instance by urlList,region,zone,user,passwd,and requestConfig.
     *
     * @param urlList
     *            service's urlList, the url's format is host:port .
     * @param region
     *            service's region
     * @param zone
     *            service's zone
     * @param user
     *            sequoiaCM user name.
     * @param passwd
     *            sequoiaCM password.
     * @param requestConfig
     *            request's configure
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public ScmConfigOption(List<String> urlList, String region, String zone, String user,
            String passwd, ScmRequestConfig requestConfig) throws ScmException {
        if (null == urlList || urlList.isEmpty()) {
            throw new ScmInvalidArgumentException("urlList is null or empty");
        }

        this.urlConfig = generateUrlConfig(region, zone, urlList);
        this.region = region;
        this.zone = zone;

        this.user = user;
        this.passwd = passwd;
        this.requestConfig = requestConfig;
    }

    /**
     * Create new instance by urlConfig,region,zone,user,passwd,and requestConfig.
     *
     * @param urlConfig
     *            service's urlConfig.
     * @param region
     *            service's region
     * @param zone
     *            service's zone
     * @param user
     *            sequoiaCM user name.
     * @param passwd
     *            sequoiaCM password.
     * @param requestConfig
     *            request's configure
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public ScmConfigOption(ScmUrlConfig urlConfig, String region, String zone, String user,
            String passwd, ScmRequestConfig requestConfig) throws ScmException {
        if (null == urlConfig) {
            throw new ScmInvalidArgumentException("urlConfig is null");
        }

        this.urlConfig = urlConfig;
        this.region = region;
        this.zone = zone;

        this.user = user;
        this.passwd = passwd;
        this.requestConfig = requestConfig;
    }

    /**
     * Create new instance by specified url list.
     *
     * @param urlList
     *            List's element has this format is host:port .
     * @throws ScmException
     *             If error happens.
     *
     * @since 2.1
     */
    public ScmConfigOption(List<String> urlList) throws ScmException {
        this(urlList, DEFAULT_REGION, DEFAULT_ZONE, null, null, null);
    }

    /**
     * Create new instance by urllist,username and passwd.
     *
     * @param urlList
     *            List's element has this formate:host:port .
     * @param user
     *            sequoiaCM user name.
     *
     * @param passwd
     *            sequoiaCM password.
     * @throws ScmException
     *             If error happens.
     * @since 2.1
     */
    public ScmConfigOption(List<String> urlList, String user, String passwd) throws ScmException {
        this(urlList, DEFAULT_REGION, DEFAULT_ZONE, user, passwd, null);
    }

    /**
     * Create new instance by urlList,user,passwd,and requestConfig.
     *
     * @param urlList
     *            List's element has this formate:host:port .
     * @param user
     *            sequoiaCM user name.
     * @param passwd
     *            sequoiaCM password.
     * @param requestConfig
     *            request's configure
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public ScmConfigOption(List<String> urlList, String user, String passwd,
            ScmRequestConfig requestConfig) throws ScmException {
        this(urlList, DEFAULT_REGION, DEFAULT_ZONE, user, passwd, requestConfig);
    }

    /**
     * Create new instance by url, username and passwd.
     *
     * @param url
     *            sequoiaCM node address, format is host:port.
     * @param user
     *            sequoiaCM user name.
     * @param passwd
     *            sequoiaCM password.
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public ScmConfigOption(String url, String user, String passwd) throws ScmException {
        this(url, DEFAULT_REGION, DEFAULT_ZONE, user, passwd, null);
    }

    /**
     * Create new instance by url,user,passwd,and requestConfig.
     *
     * @param url
     *            sequoiaCM node address, format is host:port.
     * @param user
     *            sequoiaCM user name.
     * @param passwd
     *            sequoiaCM password.
     * @param requestConfig
     *            request's configure
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public ScmConfigOption(String url, String user, String passwd, ScmRequestConfig requestConfig)
            throws ScmException {
        this(url, DEFAULT_REGION, DEFAULT_ZONE, user, passwd, requestConfig);
    }

    public ScmConfigOption(String url) throws ScmException {
        this(url, DEFAULT_REGION, DEFAULT_ZONE, null, null, null);
    }

    /**
     * Create new instance with empty properties.
     *
     * @since 2.1
     */
    public ScmConfigOption() {
    }

    public String transformUrl(String url) throws ScmException {
        if (null == url) {
            throw new ScmInvalidArgumentException("url is null");
        }
        int index = url.indexOf('/');
        if (index > -1) {
            return url.substring(0, index) + url.substring(index).toLowerCase();
        }
        return url;
    }

    private ScmUrlConfig generateUrlConfig(String region, String zone, List<String> urlList)
            throws ScmInvalidArgumentException {
        return ScmUrlConfig.custom().addUrl(region, zone, urlList).build();
    }

    /**
     * Set the value of the User property.
     *
     * @param user
     *            sequoiaCM user name.
     * @return {@code this}
     * @since 2.1
     */
    public ScmConfigOption setUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * Set the value of the Password property.
     *
     * @param passwd
     *            sequoiaCM password.
     * @return {@code this}
     * @since 2.1
     */
    public ScmConfigOption setPasswd(String passwd) {
        this.passwd = passwd;
        return this;
    }

    /**
     * Add sequoiaCM url.
     *
     * @param url
     *            sequoiaCM node address, format is host:port.
     * @return {@code this}
     * @throws ScmException
     *             If error happens.
     * @since 3.0
     */
    public ScmConfigOption addUrl(String url) throws ScmException {
        if (null == url) {
            throw new ScmInvalidArgumentException("url is null");
        }

        List<String> urlList = new ArrayList<String>();
        urlList.add(url);

        if (null == urlConfig) {
            urlConfig = generateUrlConfig(region, zone, urlList);
        }
        else {
            urlConfig.addUrl(region, zone, urlList);
        }

        return this;
    }

    /**
     * Returns the region of service.
     *
     * @return region.
     * @since 3.0
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the zone of service.
     *
     * @return zone.
     * @since 3.0
     */
    public String getZone() {
        return zone;
    }

    /**
     * Returns the value of the urls property.
     *
     * @return urls.
     * @since 2.1
     */
    public List<String> getUrls() {
        if(urlConfig == null) {
            return Collections.emptyList();
        }
        return urlConfig.getUrl();
    }

    /**
     * Returns the value of the urls property in the region and zone.
     *
     * @param region
     *            the region of service
     * @param zone
     *            the zone of service
     * @return urls.
     * @since 3.0
     */
    public List<String> getUrls(String region, String zone) {
        if(urlConfig == null) {
            return Collections.emptyList();
        }
        return urlConfig.getUrl(region, zone);
    }

    /**
     * Returns the value of the urls property exclude region.
     *
     * @param region
     *            the region of service that is excluded
     * @return urls.
     * @since 3.0
     */
    public List<String> getUrlsExclude(String region) {
        if(urlConfig == null) {
            return Collections.emptyList();
        }
        return urlConfig.getUrlExclude(region);
    }

    public List<String> getUrlsIncludeRegionExcludeZone(String includedRegion,
            String excludedZone) {
        if(urlConfig == null) {
            return Collections.emptyList();
        }
        return urlConfig.getUrlsIncludeRegionExcludeZone(includedRegion, excludedZone);
    }

    /**
     * Returns the value of the User property.
     *
     * @return username.
     * @since 2.1
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the value of the Password property.
     *
     * @return password
     * @since 2.1
     */
    public String getPasswd() {
        return passwd;
    }

    /**
     * Returns the value of the requestConfig property.
     *
     * @return requestConfig
     * @since 3.0
     */
    public ScmRequestConfig getRequestConfig() {
        return requestConfig;
    }

    /**
     * Set the value of the requestConfig property.
     *
     * @param requestConfig
     *            request configure.
     * @return {@code this}
     * @since 3.0
     */
    public ScmConfigOption setRequestConfig(ScmRequestConfig requestConfig) {
        this.requestConfig = requestConfig;
        return this;
    }

    /**
     * Set the value of the urlConfig property.
     *
     * @param urlConfig
     *            url configure.
     * @return {@code this}
     * @since 3.0
     */
    public ScmConfigOption setUrlConfig(ScmUrlConfig urlConfig) {
        this.urlConfig = urlConfig;
        return this;
    }

    /**
     * Set the value of the urlConfig property.
     *
     * @return ScmUrlConfig
     * @since 3.0
     */
    public ScmUrlConfig getUrlConfig() {
        return this.urlConfig;
    }
}
