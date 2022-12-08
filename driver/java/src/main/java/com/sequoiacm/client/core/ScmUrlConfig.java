package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;

/**
 * Url config.
 */
public class ScmUrlConfig {
    private Map<String, RegionUrl> regions;
    private String targetSite;

    private ScmUrlConfig() {
        regions = new HashMap<String, RegionUrl>();
    }

    private ScmUrlConfig(ScmUrlConfig base) {
        this.regions = new HashMap<String, RegionUrl>();
        for (Entry<String, RegionUrl> otherEntry : base.regions.entrySet()) {
            this.regions.put(otherEntry.getKey(), new RegionUrl(otherEntry.getValue()));
        }
        this.targetSite = base.targetSite;
    }

    // ensure all url point to the same site
    private List<String> checkAndFormatUrl(List<String> urls) throws ScmInvalidArgumentException {
        List<String> newUrls = new ArrayList<String>();
        for (String url : urls) {
            String tmpUrl = url;
            String urlTargetSite = null;
            int i = url.lastIndexOf("/");
            if (i <= -1) {
                urlTargetSite = "";
            }
            else {
                urlTargetSite = url.substring(i + 1).toLowerCase();
                if (!ScmArgChecker.checkUriPathArg(urlTargetSite)) {
                    throw new ScmInvalidArgumentException(
                            "sitName is invalid:siteName=" + urlTargetSite);
                }
                tmpUrl = url.substring(0, i) + "/" + urlTargetSite;
            }

            if (targetSite == null) {
                targetSite = urlTargetSite;
            }
            else if (!targetSite.equals(urlTargetSite)) {
                throw new ScmInvalidArgumentException(
                        "all url should point to the same site:invalidUrl=" + url + ",exepectSite="
                                + targetSite);
            }
            i = tmpUrl.indexOf('/');
            if (i > -1) {
                tmpUrl = tmpUrl.substring(0, i) + tmpUrl.substring(i).toLowerCase();
            }
            newUrls.add(tmpUrl);
        }
        return newUrls;
    }

    /**
     * Adds gateway urls to this config instance.
     *
     * @param region
     *            gateway region.
     * @param zone
     *            gateway zone.
     * @param urlList
     *            gateway url list.
     * @throws ScmInvalidArgumentException
     *             if arguments is invalid.
     */
    public void addUrl(String region, String zone, List<String> urlList)
            throws ScmInvalidArgumentException {
        urlList = checkAndFormatUrl(urlList);
        RegionUrl regionUrl = regions.get(region);
        if (null == regionUrl) {
            regions.put(region, new RegionUrl(region, zone, urlList));
        }
        else {
            regionUrl.addUrl(zone, urlList);
        }
    }

    /**
     * Get all gateway urls.
     *
     * @return url list.
     */
    public List<String> getUrl() {
        List<String> results = new ArrayList<String>();
        for (Entry<String, RegionUrl> regionEntry : regions.entrySet()) {
            RegionUrl regionUrl = regionEntry.getValue();
            results.addAll(regionUrl.getUrl());
        }

        return results;
    }

    /**
     * Get gateway urls with specified region and zone.
     *
     * @param region
     *            region name.
     * @param zone
     *            zone name.
     * @return gateway urls.
     */
    public List<String> getUrl(String region, String zone) {
        RegionUrl regionUrl = regions.get(region);
        if (null != regionUrl) {
            return regionUrl.getUrl(zone);
        }

        return null;
    }

    /**
     * Get gateway urls excludes the given region.
     *
     * @param region
     *            region name.
     * @return gateway urls.
     */
    public List<String> getUrlExclude(String region) {
        List<String> results = new ArrayList<String>();
        for (Entry<String, RegionUrl> regionEntry : regions.entrySet()) {
            if (!regionEntry.getKey().equals(region)) {
                RegionUrl regionUrl = regionEntry.getValue();
                results.addAll(regionUrl.getUrl());
            }
        }

        return results;
    }

    /**
     * Get gateway urls with specified args.
     *
     * @param includedRegion
     *            region name.
     * @param excludedZone
     *            zone name.
     * @return gateway urls.
     */
    public List<String> getUrlsIncludeRegionExcludeZone(String includedRegion,
            String excludedZone) {
        List<String> results = new ArrayList<String>();
        RegionUrl regionUrl = regions.get(includedRegion);
        if (null != regionUrl) {
            results.addAll(regionUrl.getUrlExclude(excludedZone));
        }

        return results;
    }

    List<UrlInfo> getUrlInfo() {
        List<UrlInfo> results = new ArrayList<UrlInfo>();
        for (Entry<String, RegionUrl> regionEntry : regions.entrySet()) {
            String region = regionEntry.getKey();
            Collection<ZoneUrl> zones = regionEntry.getValue().getZones();
            for (ZoneUrl zone : zones) {
                for (String url : zone.getUrl()) {
                    results.add(new UrlInfo(url, region, zone.getName()));
                }
            }
        }
        return results;
    }

    String getTargetSite() {
        return targetSite;
    }

    /**
     * Create a builder for build urlConfig.
     *
     * @return builder.
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Create a builder for build urlConfig.
     *
     * @param base
     *            new urlConfig copy form base.
     * @return builder.
     */
    public static Builder custom(ScmUrlConfig base) {
        return new Builder(base);
    }

    /**
     * Builder for build instance of ScmUrlConfig
     */
    public static class Builder {
        private ScmUrlConfig config;

        /**
         * Create a builder.
         */
        public Builder() {
            config = new ScmUrlConfig();
        }

        /**
         * Create a builder base on a urlConfig.
         *
         * @param base
         *            urlConfig.
         */
        public Builder(ScmUrlConfig base) {
            config = new ScmUrlConfig(base);
        }

        /**
         * Adds gateway urls.
         *
         * @param region
         *            gateway region.
         * @param zone
         *            gateway zone.
         * @param urlList
         *            gateway urls.
         * @return builder.
         * @throws ScmInvalidArgumentException
         *             if argument is invalid.
         */
        public Builder addUrl(String region, String zone, List<String> urlList)
                throws ScmInvalidArgumentException {
            config.addUrl(region, zone, urlList);
            return this;
        }

        /**
         * Builds ScmUrlConfig.
         *
         * @return instance of ScmUrlConfig.
         * @throws ScmInvalidArgumentException
         *             if error happens.
         */
        public ScmUrlConfig build() throws ScmInvalidArgumentException {
            return config;
        }
    }
}

class RegionUrl {
    private String region;
    private Map<String, ZoneUrl> zones = new HashMap<String, ZoneUrl>();

    public RegionUrl(String region, String zone, List<String> urlList) {
        this.region = region;
        zones.put(zone, new ZoneUrl(zone, urlList));
    }

    public RegionUrl(RegionUrl other) {
        this.region = other.region;
        for (Entry<String, ZoneUrl> otherEntry : other.zones.entrySet()) {
            this.zones.put(otherEntry.getKey(), new ZoneUrl(otherEntry.getValue()));
        }
    }

    public void addUrl(String zone, List<String> urlList) {
        ZoneUrl zoneUrl = zones.get(zone);
        if (null == zoneUrl) {
            zones.put(zone, new ZoneUrl(zone, urlList));
            return;
        }

        zoneUrl.addUrl(urlList);
    }

    public List<String> getUrl() {
        List<String> results = new ArrayList<String>();
        for (Entry<String, ZoneUrl> zoneEntry : zones.entrySet()) {
            ZoneUrl zoneUrl = zoneEntry.getValue();
            results.addAll(zoneUrl.getUrl());
        }

        return results;
    }

    public List<String> getUrl(String zone) {
        ZoneUrl zoneUrl = zones.get(zone);
        if (null != zoneUrl) {
            return zoneUrl.getUrl();
        }

        return null;
    }

    public List<String> getUrlExclude(String zone) {
        List<String> results = new ArrayList<String>();
        for (Entry<String, ZoneUrl> zoneEntry : zones.entrySet()) {
            if (!zoneEntry.getKey().equals(zone)) {
                ZoneUrl zoneUrl = zoneEntry.getValue();
                results.addAll(zoneUrl.getUrl());
            }
        }

        return results;
    }

    public String getName() {
        return region;
    }

    Collection<ZoneUrl> getZones() {
        return zones.values();
    }
}

class ZoneUrl {
    private String zone;
    private List<String> urlList = new ArrayList<String>();

    public ZoneUrl(String zone, List<String> urlList) {
        this.zone = zone;
        this.urlList.addAll(urlList);
    }

    public ZoneUrl(ZoneUrl other) {
        this(other.zone, other.urlList);
    }

    public void addUrl(List<String> urlList) {
        this.urlList.addAll(urlList);
    }

    public List<String> getUrl() {
        return urlList;
    }

    public String getName() {
        return zone;
    }
}

class UrlInfo {
    private String url;
    private String region;
    private String zone;
    private String nodeGroup;

    public UrlInfo(String url, String region, String zone) {
        this.url = url;
        this.region = region;
        this.zone = zone;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getNodeGroup() {
        return nodeGroup;
    }

    public void setNodeGroup(String nodeGroup) {
        this.nodeGroup = nodeGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        UrlInfo urlInfo = (UrlInfo) o;

        return url != null ? url.equals(urlInfo.url) : urlInfo.url == null;
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UrlInfo{" + "url='" + url + '\'' + ", region='" + region + '\'' + ", zone='" + zone
                + '\'' + ", nodeGroup='" + nodeGroup + '\'' + '}';
    }
}
