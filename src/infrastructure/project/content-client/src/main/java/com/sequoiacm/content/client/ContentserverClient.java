package com.sequoiacm.content.client;

import java.io.IOException;
import java.io.InputStream;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.content.client.remote.ContentserverFeign;
import com.sequoiacm.content.client.remote.FeignExceptionConverter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;

import feign.Response;

public class ContentserverClient {
    private final static ScmFeignErrorDecoder errDecoder = new ScmFeignErrorDecoder(
            new FeignExceptionConverter());

    private ContentserverFeign contentserverFeign;

    private String siteName;

    public ContentserverClient(String siteName, ContentserverFeign contentserverFeign) {
        this.contentserverFeign = contentserverFeign;
        this.siteName = siteName;
    }

    private void checkResponse(String methodKey, Response response) throws ScmServerException {
        if (response.status() >= 200 && response.status() < 300) {
            return;
        }

        Exception e = errDecoder.decode(methodKey, response);
        if (e instanceof ScmServerException) {
            throw (ScmServerException) e;
        }
        else {
            throw new ScmServerException(ScmError.SYSTEM_ERROR, e.getMessage(), e);
        }
    }

    public InputStream download(String ws, String fileId, int majorVersion, int minorVersion)
            throws ScmServerException {
        Response resp = contentserverFeign.downloadFile(ws, fileId, majorVersion, minorVersion, 0,
                0, CommonDefine.File.UNTIL_END_OF_FILE);
        checkResponse("downloadFile", resp);

        try {
            return resp.body().asInputStream();
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.NETWORK_IO,
                    "failed to download file:site=" + siteName + ", fileId=" + fileId + ", version="
                            + majorVersion + "." + minorVersion,
                    e);
        }
    }

    public long countFile(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException {
        return contentserverFeign.countFile(workspaceName, scope, condition);
    }

    public ScmFileInfo getFileInfo(String wsName, String fileId, int majorVersion, int minorVersion)
            throws ScmServerException {
        try {
            BSONObject file = contentserverFeign.getFileInfo(fileId, wsName, majorVersion,
                    minorVersion);
            return new ScmFileInfo(file);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    public ScmEleCursor<ScmFileInfo> listFile(String workspace_name, BSONObject condition,
            int scope, BSONObject orderby, long skip, long limit) throws ScmServerException {
        BasicBSONObject selector = new BasicBSONObject();
        selector.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, null);
        selector.put(FieldName.FIELD_CLFILE_INNER_USER, null);
        selector.put(FieldName.FIELD_CLFILE_NAME, null);
        selector.put(FieldName.FIELD_CLFILE_ID, null);
        selector.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, null);
        selector.put(FieldName.FIELD_CLFILE_MINOR_VERSION, null);
        selector.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, null);
        selector.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST, null);
        selector.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA, null);
        selector.put(FieldName.FIELD_CLFILE_FILE_SIZE, null);
        Response resp = contentserverFeign.fileList(workspace_name, condition, scope, orderby, skip,
                limit, selector);
        checkResponse("listFile", resp);
        InputStream is;
        try {
            is = resp.body().asInputStream();
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.NETWORK_IO,
                    "failed to list file:site=" + siteName + ", condition=" + condition, e);
        }
        return new ScmEleCursor<ScmFileInfo>(is) {
            @Override
            protected ScmFileInfo convert(BSONObject b) throws ScmServerException {
                return new ScmFileInfo(b);
            }
        };
    }

    public boolean updateFileExternalData(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData) throws ScmServerException {
        return contentserverFeign.updateFileExternalData(workspaceName, fileId, majorVersion,
                minorVersion, externalData);
    }

    public void updateFileExternalData(String ws, BSONObject matcher, BSONObject extData)
            throws ScmServerException {
        contentserverFeign.updateFileExternalData(extData, matcher, ws);
    }

}
