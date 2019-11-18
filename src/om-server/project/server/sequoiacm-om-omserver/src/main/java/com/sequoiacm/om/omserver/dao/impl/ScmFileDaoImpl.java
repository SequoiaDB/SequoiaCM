package com.sequoiacm.om.omserver.dao.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.om.omserver.dao.ScmFileDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileContent;
import com.sequoiacm.om.omserver.module.OmFileDataSiteInfo;
import com.sequoiacm.om.omserver.module.OmFileDetail;
import com.sequoiacm.om.omserver.session.ScmOmSessionImpl;

public class ScmFileDaoImpl implements ScmFileDao {
    // private static Logger logger =
    // LoggerFactory.getLogger(ScmFileDaoImpl.class);

    private ScmOmSessionImpl session;

    public ScmFileDaoImpl(ScmOmSessionImpl session) {
        this.session = session;
    }

    @Override
    public long countFile(String wsName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
            return ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT,
                    new BasicBSONObject());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count file, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public OmFileDetail getFileDetail(String ws, String fileId, int majorVersion, int minorVersion)
            throws ScmInternalException, ScmOmServerException {
        ScmSession con = session.getConnection();
        try {
            ScmWorkspace w = ScmFactory.Workspace.getWorkspace(ws, con);
            ScmFile file = ScmFactory.File.getInstance(w, new ScmId(fileId), majorVersion,
                    minorVersion);
            return transformToFileDetail(file, con);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get file info, " + e.getMessage(), e);
        }
    }

    @Override
    public List<OmFileBasic> getFileList(String ws, BSONObject condition, long skip, long limit)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmCursor<ScmFileBasicInfo> cursor = null;
        try {
            ScmWorkspace w = ScmFactory.Workspace.getWorkspace(ws, con);
            List<OmFileBasic> res = new ArrayList<>();
            cursor = ScmFactory.File.listInstance(w, ScopeType.SCOPE_CURRENT, condition,
                    ScmQueryBuilder.start(FieldName.FIELD_CLFILE_ID).is(1).get(), skip, limit);
            while (cursor.hasNext()) {
                ScmFileBasicInfo file = cursor.getNext();
                res.add(transformToFileBasic(file));
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

    private OmFileBasic transformToFileBasic(ScmFileBasicInfo file) {
        OmFileBasic fileBasic = new OmFileBasic();
        fileBasic.setCreateTime(file.getCreateDate());
        fileBasic.setId(file.getFileId().get());
        fileBasic.setMajorVersion(file.getMajorVersion());
        fileBasic.setMimeType(file.getMimeType());
        fileBasic.setMinorVersion(file.getMinorVersion());
        fileBasic.setName(file.getFileName());
        fileBasic.setUser(file.getUser());
        return fileBasic;
    }

    private OmFileDetail transformToFileDetail(ScmFile file, ScmSession con)
            throws ScmException, ScmInternalException, ScmOmServerException {
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
        fileDetail.setDirectoryId(file.getDirectory().getId());
        fileDetail.setId(file.getFileId().get());
        fileDetail.setMajorVersion(file.getMajorVersion());
        fileDetail.setMinorVersion(file.getMinorVersion());
        fileDetail.setMimeType(file.getMimeType());
        fileDetail.setName(file.getFileName());
        fileDetail.setSize(file.getSize());
        fileDetail
                .setTags(file.getTags() == null ? new HashSet<String>() : file.getTags().toSet());
        fileDetail.setTitle(file.getTitle());
        fileDetail.setUpdateTime(file.getUpdateTime());
        fileDetail.setUpdateUser(file.getUpdateUser());
        fileDetail.setUser(file.getUser());

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
