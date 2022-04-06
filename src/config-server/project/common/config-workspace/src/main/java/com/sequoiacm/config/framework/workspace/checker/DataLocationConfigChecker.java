package com.sequoiacm.config.framework.workspace.checker;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.workspace.metasource.SysSiteMetaService;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataLocationConfigChecker {

    private static final String VERSION_STR = "version";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private SysSiteMetaService sysSiteMetaService;

    private static final List<CheckItem> checkItems = new ArrayList<>();

    static {
        List<String> s3_v3_1_4 = new ArrayList<>();
        s3_v3_1_4.add(FieldName.FIELD_CLWORKSPACE_OBJECT_SHARDING_TYPE);
        s3_v3_1_4.add(FieldName.FIELD_CLWORKSPACE_BUCKET_NAME);
        checkItems.add(new CheckItem("3.1.4",
                CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR, s3_v3_1_4));
    }

    public void check(BasicBSONList dataLocations) throws MetasourceException {
        for (CheckItem checkItem : checkItems) {
            check(checkItem, dataLocations);
        }
    }

    private void check(CheckItem checkItem, BasicBSONList dataLocations)
            throws MetasourceException {
        TableDao sysSiteTable = sysSiteMetaService.getSysSiteTable();
        for (String configName : checkItem.getConfigNames()) {
            for (Object dataLocation : dataLocations) {
                BasicBSONObject location = (BasicBSONObject) dataLocation;
                if (!location.containsField(configName)) {
                    continue;
                }

                Integer siteId = BsonUtils.getIntegerChecked(location,
                        FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID);
                BSONObject siteObj = sysSiteTable.queryOne(
                        new BasicBSONObject(FieldName.FIELD_CLSITE_ID, siteId), null, null);
                if (siteObj == null) {
                    throw new IllegalArgumentException("site is not exist:site id=" + siteId);
                }
                BSONObject dataConfig = BsonUtils.getBSONChecked(siteObj,
                        FieldName.FIELD_CLSITE_DATA);
                String dataSourceType = BsonUtils.getStringChecked(dataConfig,
                        FieldName.FIELD_CLSITE_DATA_TYPE);
                if (checkItem.getType().equals(dataSourceType)) {
                    String siteName = BsonUtils.getStringChecked(siteObj,
                            FieldName.FIELD_CLSITE_NAME);
                    List<ServiceInstance> nodes = getNodesLessThanVersion(
                            checkItem.getMinimumVersion(), siteName);
                    if (nodes.size() > 0) {
                        throw new IllegalArgumentException(
                                "the version of content-server is to low to support the use of configuration:"
                                        + configName + ", allowed version: >="
                                        + checkItem.getMinimumVersion() + " ,please upgrade these nodes: "
                                        + toNodesStr(nodes));
                    }

                }

            }

        }
    }

    private String toNodesStr(List<ServiceInstance> nodes) {
        StringBuilder nodesStr = new StringBuilder();
        nodesStr.append("[");
        for (int i = 0; i < nodes.size(); i++) {
            ServiceInstance node = nodes.get(i);
            String version = node.getMetadata().get(VERSION_STR);
            if (version == null || version.isEmpty()) {
                version = "no version";
            }
            nodesStr.append(node.getHost()).append(":").append(node.getPort());
            nodesStr.append("(").append(version).append(")");
            if (i < nodes.size() - 1) {
                nodesStr.append(",");
            }
        }
        nodesStr.append("]");
        return nodesStr.toString();
    }

    private List<ServiceInstance> getNodesLessThanVersion(String minimumVersion, String siteName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(siteName.toLowerCase());
        List<ServiceInstance> result = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            String version = instance.getMetadata().get(VERSION_STR);
            if (version == null || version.isEmpty()
                    || compareVersion(version, minimumVersion) < 0) {
                result.add(instance);
            }
        }
        return result;
    }

    /**
     * 
     * @param version1
     * @param version2
     * @return version1>version2 : 1, version1=version2 : 0, version1<version2 : -1
     */
    private int compareVersion(String version1, String version2) {
        // 去除非数字字符和"."
        version1 = version1.replaceAll("[^\\d.]", "");
        version2 = version2.replaceAll("[^\\d.]", "");

        String[] s1 = version1.split("\\.");
        String[] s2 = version2.split("\\.");
        int len1 = s1.length;
        int len2 = s2.length;
        int i, j;
        for (i = 0, j = 0; i < len1 && j < len2; i++, j++) {
            if (Integer.parseInt(s1[i]) > Integer.parseInt(s2[j])) {
                return 1;
            }
            else if (Integer.parseInt(s1[i]) < Integer.parseInt(s2[j])) {
                return -1;
            }
        }
        while (i < len1) {
            if (Integer.parseInt(s1[i]) != 0) {
                return 1;
            }
            i++;
        }
        while (j < len2) {
            if (Integer.parseInt(s2[j]) != 0) {
                return -1;
            }
            j++;
        }
        return 0;
    }

    static class CheckItem {
        private String minimumVersion;
        private String type;
        private List<String> configNames;

        public CheckItem(String minimumVersion, String type, List<String> configNames) {
            this.minimumVersion = minimumVersion;
            this.type = type;
            this.configNames = configNames;
        }

        public String getMinimumVersion() {
            return minimumVersion;
        }

        public void setMinimumVersion(String minimumVersion) {
            this.minimumVersion = minimumVersion;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getConfigNames() {
            return configNames;
        }

        public void setConfigNames(List<String> configNames) {
            this.configNames = configNames;
        }
    }

}
