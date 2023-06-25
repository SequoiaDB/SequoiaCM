package com.sequoiacm.contentserver.common;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.sequoiacm.datasource.metadata.sftp.SftpDataLocation;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataSourceType;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.metadata.HadoopSiteUrl;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;
import com.sequoiacm.datasource.metadata.cephswift.CephSwiftDataLocation;
import com.sequoiacm.datasource.metadata.hbase.HbaseDataLocation;
import com.sequoiacm.datasource.metadata.hdfs.HdfsDataLocation;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;

public class ScmContentLocationResolver {

    private static final Map<ScmDataSourceType, Resolver> resolverMap = new HashMap<>();

    static {
        // sequoiadb
        resolverMap.put(ScmDataSourceType.SEQUOIADB, new Resolver() {
            @Override
            public BSONObject resolve(int siteId, ScmWorkspaceInfo wsInfo,
                    Map<Integer, ScmSite> allSite, Date createTime, String dataId, int ws_version,
                    String tableName) throws ScmServerException {
                BasicBSONObject contentLocation = new BasicBSONObject();
                ScmLocation scmLocation = wsInfo.getSiteDataLocation(siteId, ws_version);
                contentLocation.put(FieldName.ContentLocation.FIELD_TYPE,
                        ScmDataSourceType.SEQUOIADB.getName());
                contentLocation.put(FieldName.ContentLocation.FIELD_SITE, siteId);

                SdbDataLocation sdbDataLocation = (SdbDataLocation) scmLocation;
                String timezone = ScmIdParser.getTimezoneName(dataId);
                String cl = sdbDataLocation.getDataClName(createTime, timezone);
                String cs = sdbDataLocation.getDataCsName(wsInfo.getName(), createTime, timezone);
                contentLocation.put(FieldName.ContentLocation.FIELD_CL, cl);
                contentLocation.put(FieldName.ContentLocation.FIELD_CS, cs);
                contentLocation.put(FieldName.ContentLocation.FIELD_LOB_ID, dataId);

                List<String> urls = allSite.get(siteId).getDataUrl().getUrls();
                contentLocation.put(FieldName.ContentLocation.FIELD_URLS, toBsonList(urls));
                return contentLocation;
            }
        });

        // ceph s3
        resolverMap.put(ScmDataSourceType.CEPH_S3, new Resolver() {

            @Override
            public BSONObject resolve(int siteId, ScmWorkspaceInfo wsInfo,
                    Map<Integer, ScmSite> allSite, Date createTime, String dataId, int ws_version,
                    String tableName) throws ScmServerException {
                BasicBSONObject contentLocation = new BasicBSONObject();
                contentLocation.put(FieldName.ContentLocation.FIELD_SITE, siteId);
                contentLocation.put(FieldName.ContentLocation.FIELD_TYPE,
                        ScmDataSourceType.CEPH_S3.getName());

                ScmLocation scmLocation = wsInfo.getSiteDataLocation(siteId, ws_version);
                CephS3DataLocation cephS3DataLocation = (CephS3DataLocation) scmLocation;
                String timezone = ScmIdParser.getTimezoneName(dataId);
                String objectId = cephS3DataLocation.getObjectId(dataId, wsInfo.getName(),
                        createTime, timezone);
                String bucketName = tableName;
                if (Strings.isNullOrEmpty(tableName)) {
                    bucketName = cephS3DataLocation.getBucketName(wsInfo.getName(), createTime,
                            timezone);
                }
                contentLocation.put(FieldName.ContentLocation.FIELD_OBJECT_ID, objectId);
                contentLocation.put(FieldName.ContentLocation.FIELD_BUCKET, bucketName);
                contentLocation.put(FieldName.ContentLocation.FIELD_URLS,
                        toBsonList(allSite.get(siteId).getDataUrl().getUrls()));
                return contentLocation;
            }
        });

        // ceph swift
        resolverMap.put(ScmDataSourceType.CEPH_SWIFT, new Resolver() {
            @Override
            public BSONObject resolve(int siteId, ScmWorkspaceInfo wsInfo,
                    Map<Integer, ScmSite> allSite, Date createTime, String dataId, int ws_version,
                    String tableName) throws ScmServerException {
                BasicBSONObject contentLocation = new BasicBSONObject();
                contentLocation.put(FieldName.ContentLocation.FIELD_SITE, siteId);
                contentLocation.put(FieldName.ContentLocation.FIELD_TYPE,
                        ScmDataSourceType.CEPH_SWIFT.getName());

                ScmLocation scmLocation = wsInfo.getSiteDataLocation(siteId, ws_version);
                CephSwiftDataLocation cephSwiftDataLocation = (CephSwiftDataLocation) scmLocation;
                String containerName = cephSwiftDataLocation.getContainerName(wsInfo.getName(),
                        createTime, ScmIdParser.getTimezoneName(dataId));
                contentLocation.put(FieldName.ContentLocation.FIELD_CONTAINER, containerName);
                contentLocation.put(FieldName.ContentLocation.FIELD_OBJECT_ID, dataId);
                contentLocation.put(FieldName.ContentLocation.FIELD_URLS,
                        toBsonList(allSite.get(siteId).getDataUrl().getUrls()));
                return contentLocation;
            }
        });

        // hbase
        resolverMap.put(ScmDataSourceType.HBASE, new Resolver() {
            @Override
            public BSONObject resolve(int siteId, ScmWorkspaceInfo wsInfo,
                    Map<Integer, ScmSite> allSite, Date createTime, String dataId, int ws_version,
                    String tableName) throws ScmServerException {
                BasicBSONObject contentLocation = new BasicBSONObject();
                contentLocation.put(FieldName.ContentLocation.FIELD_SITE, siteId);
                contentLocation.put(FieldName.ContentLocation.FIELD_TYPE,
                        ScmDataSourceType.HBASE.getName());

                ScmLocation scmLocation = wsInfo.getSiteDataLocation(siteId, ws_version);
                HbaseDataLocation hbaseDataLocation = (HbaseDataLocation) scmLocation;
                String hbaseTableName = hbaseDataLocation.getTableName(wsInfo.getName(),
                        createTime, ScmIdParser.getTimezoneName(dataId));
                contentLocation.put(FieldName.ContentLocation.FIELD_TABLE_NAME, hbaseTableName);
                contentLocation.put(FieldName.ContentLocation.FIELD_FILE_NAME, dataId);
                HadoopSiteUrl siteUrl = (HadoopSiteUrl) allSite.get(siteId).getDataUrl();
                String urls = siteUrl.getDataConf().get("hbase.zookeeper.quorum");
                contentLocation.put(FieldName.ContentLocation.FIELD_URLS, toBsonList(urls));
                return contentLocation;
            }
        });

        // hdfs
        resolverMap.put(ScmDataSourceType.HDFS, new Resolver() {
            @Override
            public BSONObject resolve(int siteId, ScmWorkspaceInfo wsInfo,
                    Map<Integer, ScmSite> allSite, Date createTime, String dataId, int ws_version,
                    String tableName) throws ScmServerException {
                BasicBSONObject contentLocation = new BasicBSONObject();
                contentLocation.put(FieldName.ContentLocation.FIELD_SITE, siteId);
                contentLocation.put(FieldName.ContentLocation.FIELD_TYPE,
                        ScmDataSourceType.HDFS.getName());

                ScmLocation scmLocation = wsInfo.getSiteDataLocation(siteId, ws_version);
                HdfsDataLocation hdfsDataLocation = (HdfsDataLocation) scmLocation;
                String directory = hdfsDataLocation.getDirectory(wsInfo.getName(), createTime,
                        ScmIdParser.getTimezoneName(dataId));
                contentLocation.put(FieldName.ContentLocation.FIELD_DIRECTORY, directory);
                contentLocation.put(FieldName.ContentLocation.FIELD_FILE_NAME, dataId);
                HadoopSiteUrl siteUrl = (HadoopSiteUrl) allSite.get(siteId).getDataUrl();
                String urls = siteUrl.getDataConf().get("fs.defaultFS");
                contentLocation.put(FieldName.ContentLocation.FIELD_URLS, toBsonList(urls));
                return contentLocation;
            }
        });

        // sftp
        resolverMap.put(ScmDataSourceType.SFTP, new Resolver() {
            @Override
            public BSONObject resolve(int siteId, ScmWorkspaceInfo wsInfo,
                    Map<Integer, ScmSite> allSite, Date createTime, String dataId, int ws_version,
                    String tableName) throws ScmServerException {
                BasicBSONObject contentLocation = new BasicBSONObject();
                contentLocation.put(FieldName.ContentLocation.FIELD_SITE, siteId);
                contentLocation.put(FieldName.ContentLocation.FIELD_TYPE,
                        ScmDataSourceType.SFTP.getName());

                ScmLocation scmLocation = wsInfo.getSiteDataLocation(siteId, ws_version);
                SftpDataLocation sftpDataLocation = (SftpDataLocation) scmLocation;
                String filePath = sftpDataLocation.getFilePath(wsInfo.getName(), createTime,
                        dataId);
                contentLocation.put(FieldName.ContentLocation.FIELD_FILE_PATH, filePath);
                List<String> urls = allSite.get(siteId).getDataUrl().getUrls();
                contentLocation.put(FieldName.ContentLocation.FIELD_URLS, toBsonList(urls));
                return contentLocation;
            }
        });

    }

    public static Resolver getResolver(String dataSourceType) {
        ScmDataSourceType type = ScmDataSourceType.getDataSourceType(dataSourceType);
        if (type == null) {
            throw new IllegalArgumentException("unknown dataSourceType:" + dataSourceType);
        }
        Resolver resolver = resolverMap.get(type);
        if (resolver == null) {
            throw new IllegalArgumentException("resolver is not found,type=" + dataSourceType);
        }
        return resolver;
    }

    private static BasicBSONList toBsonList(List<String> list) {
        BasicBSONList basicBSONList = new BasicBSONList();
        basicBSONList.addAll(list);
        return basicBSONList;
    }

    private static BasicBSONList toBsonList(String urls) {
        BasicBSONList basicBSONList = new BasicBSONList();
        basicBSONList.addAll(Arrays.asList(urls.split(",")));
        return basicBSONList;
    }

    public interface Resolver {
        BSONObject resolve(int siteId, ScmWorkspaceInfo wsInfo, Map<Integer, ScmSite> allSite,
                Date createTime, String dataId, int ws_version, String tableName)
                throws ScmServerException;
    }

}
