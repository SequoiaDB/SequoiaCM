package com.sequoiacm.om.omserver.dao.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.om.omserver.module.*;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.om.omserver.dao.ScmFileDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import org.springframework.util.StringUtils;

public class ScmFileDaoImpl implements ScmFileDao {
    // private static Logger logger =
    // LoggerFactory.getLogger(ScmFileDaoImpl.class);

    private ScmOmSession session;

    public ScmFileDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public long countFile(String wsName, int scope, BSONObject condition)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            return ScmFactory.File.countInstance(ws, ScopeType.getScopeType(scope), condition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count file, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public OmFileDetail getFileDetail(String wsName, String fileId, int majorVersion,
            int minorVersion) throws ScmInternalException, ScmOmServerException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            ScmFile file = ScmFactory.File.getInstance(ws, new ScmId(fileId), majorVersion,
                    minorVersion);
            OmFileDetail fileDetail = transformToFileDetail(file);
            // set class name
            if (!StringUtils.isEmpty(fileDetail.getClassId())) {
                ScmClass scmClass = ScmFactory.Class.getInstance(ws,
                        new ScmId(fileDetail.getClassId()));
                fileDetail.setClassName(scmClass.getName());
            }
            // set batch name
            if (!StringUtils.isEmpty(fileDetail.getBatchId())) {
                ScmBatch batch = ScmFactory.Batch.getInstance(ws,
                        new ScmId(fileDetail.getBatchId()));
                fileDetail.setBatchName(batch.getName());
            }
            // set directory path
            if (ws.isEnableDirectory()) {
                ScmDirectory directory = file.getDirectory();
                fileDetail.setDirectoryId(directory.getId());
                fileDetail.setDirectoryPath(directory.getPath());
            }
            return fileDetail;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get file info, " + e.getMessage(), e);
        }
    }

    @Override
    public List<OmFileBasic> getFileList(String wsName, int scope, BSONObject condition,
            BSONObject orderBy, long skip, long limit) throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmCursor<ScmFileBasicInfo> cursor = null;
        try {
            ScopeType scopeType = ScopeType.getScopeType(scope);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            cursor = ScmFactory.File.listInstance(ws, scopeType, condition, orderBy, skip, limit);
            List<OmFileBasic> res = new ArrayList<>();
            while (cursor.hasNext()) {
                ScmFileBasicInfo basicInfo = cursor.getNext();
                ScmFile file = ScmFactory.File.getInstance(ws, basicInfo.getFileId(),
                        basicInfo.getMajorVersion(), basicInfo.getMinorVersion());
                res.add(transformToFileBasicInfo(file));
            }
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get file list, " + e.getMessage(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void uploadFile(String wsName, OmFileInfo fileInfo, BSONObject uploadConfig,
            InputStream is) throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmOutputStream scmOs = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            // 创建文件实例
            ScmFile scmFile = ScmFactory.File.createInstance(ws);
            // 设置文件属性
            scmFile.setFileName(fileInfo.getName());
            scmFile.setMimeType(getMimeTypeByFileName(scmFile.getFileName()));
            scmFile.setTitle(fileInfo.getTitle());
            scmFile.setAuthor(fileInfo.getAuthor());
            scmFile.setDirectory(fileInfo.getDirectoryId());
            // 设置标签
            ScmTags scmTags = new ScmTags();
            scmTags.addTags(fileInfo.getTags());
            scmFile.setTags(scmTags);
            // 设置元数据
            if (!StringUtils.isEmpty(fileInfo.getClassId())) {
                ScmClassProperties classProperties = new ScmClassProperties(fileInfo.getClassId());
                classProperties.addProperties(fileInfo.getClassProperties());
                scmFile.setClassProperties(classProperties);
            }
            scmFile.setContent(is);
            scmFile.save(getScmUploadConf(uploadConfig));
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to upload file, " + e.getMessage(),
                    e);
        }
        finally {
            if (scmOs != null) {
                scmOs.cancel();
            }
        }
    }

    private String getMimeTypeByFileName(String fileName) {
        String[] nameSplit = fileName.split("\\.");
        MimeType mimeType = null;
        if (1 < nameSplit.length) {
            mimeType = MimeType.getBySuffix(nameSplit[nameSplit.length - 1]);
        }
        return mimeType == null ? "" : mimeType.getType();
    }

    @Override
    public OmFileContent downloadFile(String wsName, String fileId, int majorVersion,
            int minorVersion) throws ScmInternalException, ScmOmServerException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            ScmFile scmFile = ScmFactory.File.getInstance(ws, new ScmId(fileId), majorVersion,
                    minorVersion);
            ScmInputStream is = ScmFactory.File.createInputStream(scmFile);
            try {
                ScmInputStreamWrapper isWrapper = new ScmInputStreamWrapper(is);
                return new OmFileContent(scmFile.getFileName(), scmFile.getSize(), isWrapper);
            }
            catch (Exception e) {
                is.close();
                throw e;
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to download file, " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFiles(String wsName, List<String> fileIdList)
            throws ScmInternalException, ScmOmServerException {
        ScmSession conn = session.getConnection();
        int curIdx = 0;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, conn);
            for (; curIdx < fileIdList.size(); curIdx++) {
                String fileId = fileIdList.get(curIdx);
                ScmFactory.File.deleteInstance(ws, new ScmId(fileId), true);
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to delete files, success count=" + curIdx + ", failed count="
                            + (fileIdList.size() - curIdx) + ", detail:" + e.getMessage(),
                    e);
        }
    }

    @Override
    public void updateFileContent(String wsName, String id, BSONObject updateContentOption,
            InputStream newFileContent) throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, con);
            ScmFile scmFile = ScmFactory.File.getInstance(ws, new ScmId(id));
            scmFile.updateContent(newFileContent, getScmUpdateContentOption(updateContentOption));
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to update file content, " + e.getMessage(), e);
        }

    }

    private ScmUploadConf getScmUploadConf(BSONObject uploadConfig) {
        Boolean isOverwrite = BsonUtils.getBooleanOrElse(uploadConfig,
                CommonDefine.RestArg.FILE_IS_OVERWRITE, false);
        Boolean isNeedMd5 = BsonUtils.getBooleanOrElse(uploadConfig,
                CommonDefine.RestArg.FILE_IS_NEED_MD5, false);
        return new ScmUploadConf(isOverwrite, isNeedMd5);
    }

    private ScmUpdateContentOption getScmUpdateContentOption(BSONObject updateContentOption) {
        Boolean isNeedMd5 = BsonUtils.getBooleanOrElse(updateContentOption,
                CommonDefine.RestArg.FILE_IS_NEED_MD5, false);
        return new ScmUpdateContentOption(isNeedMd5);
    }

    private OmFileBasic transformToFileBasicInfo(ScmFile file) {
        OmFileBasic omFileBasic = new OmFileBasic();
        omFileBasic.setId(file.getFileId().get());
        omFileBasic.setName(file.getFileName());
        omFileBasic.setMimeType(file.getMimeType());
        omFileBasic.setSize(file.getSize());
        omFileBasic.setUser(file.getUser());
        omFileBasic.setCreateTime(file.getCreateTime());
        omFileBasic.setUpdateTime(file.getUpdateTime());
        omFileBasic.setMajorVersion(file.getMajorVersion());
        omFileBasic.setMinorVersion(file.getMinorVersion());
        return omFileBasic;
    }

    private OmFileDetail transformToFileDetail(ScmFile file) throws ScmException {
        OmFileDetail fileDetail = new OmFileDetail();
        fileDetail.setAuthor(file.getAuthor());
        fileDetail.setBatchId(file.getBatchId() == null ? "" : file.getBatchId().get());
        fileDetail.setClassId(file.getClassId() == null ? "" : file.getClassId().get());
        fileDetail.setClassProperties(
                file.getClassProperties() == null ? new HashMap<String, Object>()
                        : file.getClassProperties().toMap());
        fileDetail.setCreateTime(file.getCreateTime());
        fileDetail.setDataCreateTime(file.getDataCreateTime());
        fileDetail.setDataId(file.getDataId().get());
        fileDetail.setId(file.getFileId().get());
        fileDetail.setMajorVersion(file.getMajorVersion());
        fileDetail.setMinorVersion(file.getMinorVersion());
        fileDetail.setMimeType(file.getMimeType());
        fileDetail.setName(file.getFileName());
        fileDetail.setSize(file.getSize());
        fileDetail.setMd5(file.getMd5());
        fileDetail.setTags(file.getTags() == null ? new HashSet<String>() : file.getTags().toSet());
        fileDetail.setTitle(file.getTitle());
        fileDetail.setUpdateTime(file.getUpdateTime());
        fileDetail.setUpdateUser(file.getUpdateUser());
        fileDetail.setUser(file.getUser());
        fileDetail.setMimeType(file.getMimeType());
        ;

        List<OmFileDataSiteInfo> sites = new ArrayList<>();
        for (ScmFileLocation location : file.getLocationList()) {
            OmFileDataSiteInfo site = new OmFileDataSiteInfo();
            site.setSiteId(location.getSiteId());
            site.setLastAccessTime(location.getDate());
            site.setCreateTime(location.getCreateDate());
            sites.add(site);
        }

        fileDetail.setSites(sites);
        return fileDetail;
    }

}

class ScmInputStreamWrapper extends InputStream {
    private ScmInputStream innerIs;

    public ScmInputStreamWrapper(ScmInputStream fileIs) {
        this.innerIs = fileIs;
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException("did not impl read() function yet!");
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return innerIs.read(b, off, len);
        }
        catch (ScmException e) {
            throw new IOException("failed to read file content from scm", e);
        }
    }
}
