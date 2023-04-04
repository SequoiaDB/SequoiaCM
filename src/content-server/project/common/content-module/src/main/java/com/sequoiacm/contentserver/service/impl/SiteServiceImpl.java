package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.service.ISiteService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SiteServiceImpl implements ISiteService {
    private static final Logger logger = LoggerFactory.getLogger(SiteServiceImpl.class);
    @Override
    public BSONObject getSite(String siteName) throws ScmServerException {
        MetaAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource().getSiteAccessor();
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLSITE_NAME, siteName);
        BSONObject site = ScmMetaSourceHelper.queryOne(accessor, matcher);
        if(site == null) {
            throw new ScmServerException(ScmError.SITE_NOT_EXIST, "site not exist:siteName=" + siteName);
        }
        return site;
    }

    @Override
    public MetaCursor getSiteList(BSONObject condition, long skip, long limit)
            throws ScmServerException {
        MetaAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                .getSiteAccessor();
        try {
            return accessor.query(condition, null, null, skip, limit);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get site list, condition=" + condition, e);
        }
    }

    @Override
    public long countSite(BSONObject condition) throws ScmServerException {
        try {
            MetaAccessor accessor = ScmContentModule.getInstance().getMetaService().getMetaSource()
                    .getSiteAccessor();
            return accessor.count(condition);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to get site count, condition=" + condition, e);
        }
    }

    @Override
    public void getSecretFile(HttpServletResponse response, boolean isMeta)
            throws ScmServerException {
        ScmSite localSiteInfo = ScmContentModule.getInstance().getLocalSiteInfo();
        ScmSiteUrl siteUrl = null;
        if (isMeta) {
            siteUrl = localSiteInfo.getMetaUrl();
        }
        else {
            siteUrl = localSiteInfo.getDataUrl();
        }
        String datasourceType = siteUrl.getType();
        if (datasourceType.equals(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR)
                || datasourceType
                        .equals(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR)) {
            // hbase hdfs 无密码
            response.setHeader("size", String.valueOf(0));
            return;
        }
        InputStream inputStream = null;
        ServletOutputStream out = null;
        try {
            String passwordFilePath = siteUrl.getPassword();
            File passwdFile = new File(passwordFilePath);
            response.setHeader("fileName", passwdFile.getName());
            // File.toPath 返回的是一个 Path 对象，里面包含了文件路径信息，根目录名信息，文件类型信息等
            Path path = passwdFile.toPath();
            inputStream = Files.newInputStream(path);
            response.setHeader("size", String.valueOf(Files.size(path)));
            out = response.getOutputStream();
            byte[] b = new byte[1024];
            int length;
            while ((length = inputStream.read(b)) > 0) {
                out.write(b, 0, length);
            }
            out.flush();
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.FILE_IO, "Failed to get secret file", e);
        }
        finally {
            ScmSystemUtils.closeResource(out);
            ScmSystemUtils.closeResource(inputStream);
        }
    }

}
