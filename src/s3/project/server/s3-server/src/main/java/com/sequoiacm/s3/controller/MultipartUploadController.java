package com.sequoiacm.s3.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.RestParamDefine;
import com.sequoiacm.s3.core.*;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.*;
import com.sequoiacm.s3.service.BucketService;
import com.sequoiacm.s3.service.MultiPartService;
import com.sequoiacm.s3.service.ObjectService;
import com.sequoiacm.s3.utils.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.sequoiacm.s3.utils.DataFormatUtils.formatDate;

@RestController
public class MultipartUploadController {
    private final Logger logger = LoggerFactory.getLogger(MultipartUploadController.class);

    @Autowired
    ObjectService objectService;

    @Autowired
    IScmBucketService bucketService;

    @Autowired
    MultiPartService multiPartService;

    @Autowired
    RestUtils restUtils;

    @PostMapping(value = "/{bucketname:.+}/**", params = RestParamDefine.UPLOADS, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity initMultiPartUpload(@PathVariable("bucketname") String bucketName,
            HttpServletRequest httpServletRequest, ScmSession session) throws S3ServerException {
        try {
            logger.debug("initMultiPartUploadObject. bucketName={}, requestURI={}", bucketName,
                    httpServletRequest.getRequestURI());
            String objectName = restUtils.getObjectNameByURI(httpServletRequest.getRequestURI());

            UploadMeta uploadMeta = new UploadMeta(httpServletRequest);
            uploadMeta.setKey(objectName);
            checkMeta(uploadMeta);

            InitiateMultipartUploadResult result = multiPartService.initMultipartUpload(session,
                    bucketName, uploadMeta);
            return ResponseEntity.ok().body(result);
        }
        catch (S3ServerException e) {
            logger.error("initMultiPartUploadObject failed. bucketName={}, requestURI={}",
                    bucketName, httpServletRequest.getRequestURI());
            throw e;
        }
    }

    @PutMapping(value = "/{bucketname:.+}/**", params = RestParamDefine.PARTNUMBER, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity uploadPart(@PathVariable("bucketname") String bucketName,
            @RequestHeader(name = RestParamDefine.PutObjectHeader.CONTENT_MD5, required = false) String contentMD5,
            @RequestParam(RestParamDefine.PARTNUMBER) String partNumberStr,
            @RequestParam(RestParamDefine.UPLOADID) String uploadIdStr,
            HttpServletRequest httpServletRequest, ScmSession session) throws S3ServerException {
        try {
            logger.debug("upload part. bucketName={}, requestURI={}, uploadId={}, partNumber={}",
                    bucketName, httpServletRequest.getRequestURI(), uploadIdStr, partNumberStr);

            // CustomContextValve 可能已经对bucket，objectName，uploadId，partNumber进行了查询、解析、检查
            // 并通过attribute记录，此处通过attribute获取处理过的相应数据，减少数据库交互查询，及转换处理
            // CustomContextValve 只对数据长度较大（512k以上）的请求进行检查，
            ObjectUri objectMeta = (ObjectUri) httpServletRequest
                    .getAttribute((RestParamDefine.Attribute.S3_OBJECTURI));
            String objectName;
            if (objectMeta == null) {
                objectName = restUtils.getObjectNameByURI(httpServletRequest.getRequestURI());
            }
            else {
                objectName = objectMeta.getObjectName();
            }

            // get and check bucket
            ScmBucket bucket = (ScmBucket) httpServletRequest
                    .getAttribute(RestParamDefine.Attribute.S3_BUCKET);
            if (bucket == null) {
                bucket = getBucket(session.getUser(), bucketName);
            }

            Long uploadId = (Long) httpServletRequest
                    .getAttribute(RestParamDefine.Attribute.S3_UPLOADID);
            if (uploadId == null) {
                uploadId = restUtils.convertUploadId(uploadIdStr);
            }

            Integer partNumber = (Integer) httpServletRequest
                    .getAttribute(RestParamDefine.Attribute.S3_PARTNUMBER);
            if (partNumber == null) {
                partNumber = getPartNumber(partNumberStr);
            }

            InputStream body = httpServletRequest.getInputStream();
            Long realContentLength = 0L;
            if (httpServletRequest.getHeader("x-amz-decoded-content-length") != null) {
                body = new S3InputStreamReaderChunk(httpServletRequest.getInputStream());
                realContentLength = Long
                        .parseLong(httpServletRequest.getHeader("x-amz-decoded-content-length"));
                // 当上传方式为chunk，且实际内容长度为0时，x-amz-decoded-content-length=0
            }
            else {
                if (httpServletRequest.getHeader("content-length") != null) {
                    realContentLength = Long
                            .parseLong(httpServletRequest.getHeader("content-length"));
                }
                // 当上传实际内容长度为0时，content-length=0
            }
            Part newPart = multiPartService.uploadPart(session, bucket, objectName, uploadId,
                    partNumber, contentMD5, body, realContentLength);

            HttpHeaders headers = new HttpHeaders();
            headers.add(RestParamDefine.PutObjectResultHeader.ETAG,
                    "\"" + newPart.getEtag() + "\"");

            logger.debug(
                    "upload part success. bucketName={}, objectName={}, uploadId={}, "
                            + "partNumber={}, eTag={}, realContentLength={}",
                    bucketName, objectName, uploadId, partNumber, newPart.getEtag(),
                    realContentLength);
            return ResponseEntity.ok().headers(headers).build();
        }
        catch (S3ServerException e) {
            logger.error(
                    "upload part failed. bucketName={}, requestURI={},"
                            + " uploadId={}, partNumber={}",
                    bucketName, httpServletRequest.getRequestURI(), uploadIdStr, partNumberStr);
            restUtils.skipInputStream(httpServletRequest);
            throw e;
        }
        catch (Exception e) {
            restUtils.skipInputStream(httpServletRequest);
            throw new S3ServerException(S3Error.PART_UPLOAD_PART_FAILED,
                    "upload part failed. bucketName=" + bucketName + ", requestURI="
                            + httpServletRequest.getRequestURI() + ", uploadId=" + uploadIdStr
                            + ", partNumber=" + partNumberStr,
                    e);
        }
        finally {
            restUtils.closeInputStream(httpServletRequest);
        }
    }

    @PutMapping(value = "/{bucketname:.+}/**", params = RestParamDefine.PARTNUMBER, headers = RestParamDefine.CopyPartPara.X_AMZ_COPY_SOURCE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity uploadPartCopy(@PathVariable("bucketname") String bucketName,
            @RequestHeader(name = RestParamDefine.PutObjectHeader.CONTENT_MD5, required = false) String contentMD5,
            @RequestParam(RestParamDefine.PARTNUMBER) String partNumberStr,
            @RequestParam(RestParamDefine.UPLOADID) String uploadIdStr,
            @RequestHeader(name = RestParamDefine.CopyPartPara.X_AMZ_COPY_SOURCE) String copySource,
            @RequestHeader(name = RestParamDefine.CopyPartPara.X_AMZ_COPY_SOURCE_RANGE) String copySourceRange,
            HttpServletRequest httpServletRequest, CopyObjectMatcher matcher, ScmSession session)
            throws S3ServerException {
        GetObjectResult sourceObject = null;
        try {
            String objectName = restUtils.getObjectNameByURI(httpServletRequest.getRequestURI());

            logger.debug("upload part copy. bucketName={}, objectName={}, "
                    + "uploadId={}, partNumber={}, x-amz-copy-source={}, x-amz-copy-source-range={}",
                    bucketName, objectName, uploadIdStr, partNumberStr, copySource,
                    copySourceRange);

            ObjectUri sourceUri = new ObjectUri(copySource);
            Range range = null;
            if (copySourceRange != null) {
                range = new Range(copySourceRange);
            }

            sourceObject = objectService.getObject(session, sourceUri.getBucketName(),
                    sourceUri.getObjectName(), sourceUri.getVersionId(), matcher, range);

            long contentLength = getCopyContentLength(range, sourceObject);

            long uploadId = restUtils.convertUploadId(uploadIdStr);
            int partNumber = getPartNumber(partNumberStr);

            // get bucket
            ScmBucket bucket = getBucket(session.getUser(), bucketName);

            Part newPart = multiPartService.uploadPart(session, bucket, objectName, uploadId,
                    partNumber, contentMD5, sourceObject.getData(), contentLength);

            CopyPartResult copyResult = new CopyPartResult();
            copyResult.seteTag(newPart.getEtag());
            copyResult.setLastModified(formatDate(newPart.getLastModified()));
            HttpHeaders headers = new HttpHeaders();
            // TODO:for versioning
            // if (!(sourceObject.getMeta().getNoVersionFlag())) {
            // headers.add(RestParamDefine.CopyObjectResultHeader.SOURCE_VERSION_ID,
            // String.valueOf(sourceObject.getMeta().getVersionId()));
            // }

            logger.debug(
                    "upload part copy success. bucketName={}, objectName={}, uploadId={}, partNumber={}, source={}, range={}",
                    bucketName, objectName, uploadId, partNumber, copySource, copySourceRange);
            return ResponseEntity.ok().headers(headers).body(copyResult);
        }
        catch (S3ServerException e) {
            logger.error(
                    "upload part copy failed. bucketName={}, requestURI={}, "
                            + "source={}, range={}",
                    bucketName, httpServletRequest.getRequestURI(), copySource, copySourceRange);
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.PART_UPLOAD_PART_FAILED,
                    "upload part copy failed. " + "bucketName=" + bucketName + ", requestURI="
                            + httpServletRequest.getRequestURI() + "source=" + copySource
                            + ", range=" + copySourceRange,
                    e);
        }
        finally {
            if (sourceObject != null) {
                sourceObject.close();
            }
        }
    }

    @PostMapping(value = "/{bucketname:.+}/**", params = RestParamDefine.UPLOADID, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity completeMultiPart(@PathVariable("bucketname") String bucketName,
            @RequestParam(RestParamDefine.UPLOADID) String uploadIdStr,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            ScmSession session) throws S3ServerException, IOException {
        try {
            String objectName = restUtils.getObjectNameByURI(httpServletRequest.getRequestURI());
            logger.debug("completeMultiPart. bucketName={}, objectName={}, uploadId:{}", bucketName,
                    objectName, uploadIdStr);

            ServletOutputStream outputStream = httpServletResponse.getOutputStream();

            CompleteMultipartUpload completeMultipartUpload = getCompleteMap(httpServletRequest);

            long uploadId = restUtils.convertUploadId(uploadIdStr);
            CompleteMultipartUploadResult result = multiPartService.completeUpload(session,
                    bucketName, objectName, uploadId, completeMultipartUpload.getPart(),
                    outputStream);

            result.setLocation(httpServletRequest.getRequestURI());

            logger.debug("completeMultiPart success. bucketName={}, objectName={}, uploadId={}",
                    bucketName, objectName, uploadId);

            HttpHeaders headers = new HttpHeaders();
            if (result.getVersionId() != null) {
                headers.add(RestParamDefine.PutObjectResultHeader.VERSION_ID,
                        result.getVersionId().toString());
            }

            return ResponseEntity.ok().headers(headers).body(result);
        }
        catch (Exception e) {
            logger.error("completeMultiPart failed. bucketName={}, requestURI={}, uploadId:{}",
                    bucketName, httpServletRequest.getRequestURI(), uploadIdStr);
            throw e;
        }
    }

    @GetMapping(value = "/{bucketname:.+}/**", params = RestParamDefine.UPLOADID, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity listParts(@PathVariable("bucketname") String bucketName,
            @RequestParam(RestParamDefine.UPLOADID) String uploadIdStr,
            @RequestParam(value = RestParamDefine.ListPartsPara.PART_NUMBER_MARKER, required = false) Integer partNumberMarker,
            @RequestParam(value = RestParamDefine.ListPartsPara.MAX_PARTS, required = false, defaultValue = "1000") Integer maxParts,
            @RequestParam(value = RestParamDefine.ListPartsPara.ENCODING_TYPE, required = false) String encodingType,
            HttpServletRequest httpServletRequest, ScmSession session) throws S3ServerException {
        try {
            String objectName = restUtils.getObjectNameByURI(httpServletRequest.getRequestURI());
            logger.debug("listParts. bucketName={}, objectName={}, uploadId:{}, encodeType={}",
                    bucketName, objectName, uploadIdStr, encodingType);

            long uploadId = restUtils.convertUploadId(uploadIdStr);
            ListPartsResult result = multiPartService.listParts(session, bucketName, objectName,
                    uploadId, partNumberMarker, maxParts, encodingType);

            logger.debug(
                    "listParts success. bucketName={}, objectName={}, uploadId={}, part.size={}",
                    bucketName, objectName, uploadId, result.getPartList().size());
            return ResponseEntity.ok().body(result);
        }
        catch (Exception e) {
            logger.error("list Parts failed. bucketName={}, requestURI={}, uploadId:{}", bucketName,
                    httpServletRequest.getRequestURI(), uploadIdStr);
            throw e;
        }
    }

    @DeleteMapping(value = "/{bucketname:.+}/**", params = RestParamDefine.UPLOADID, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity abortUpload(@PathVariable("bucketname") String bucketName,
            @RequestParam(RestParamDefine.UPLOADID) String uploadIdStr,
            HttpServletRequest httpServletRequest, ScmSession session) throws S3ServerException {
        try {
            String objectName = restUtils.getObjectNameByURI(httpServletRequest.getRequestURI());
            logger.debug("abortUpload. bucketName={}, objectName={}, uploadId:{}", bucketName,
                    objectName, uploadIdStr);
            long uploadId = restUtils.convertUploadId(uploadIdStr);
            multiPartService.abortUpload(session, bucketName, objectName, uploadId);

            logger.debug("abortUpload success. bucketName={}, objectName={}, uploadId={}",
                    bucketName, objectName, uploadId);
            return ResponseEntity.noContent().build();
        }
        catch (Exception e) {
            logger.error("abortUpload failed. bucketName={}, requestURI={}, uploadId:{}",
                    bucketName, httpServletRequest.getRequestURI(), uploadIdStr);
            throw e;
        }
    }

    @GetMapping(value = "/{bucketname:.+}", params = RestParamDefine.UPLOADS, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity listUploads(@PathVariable("bucketname") String bucketName,
            @RequestParam(value = RestParamDefine.ListUploadsPara.PREFIX, required = false) String prefix,
            @RequestParam(value = RestParamDefine.ListUploadsPara.DELIMITER, required = false) String delimiter,
            @RequestParam(value = RestParamDefine.ListUploadsPara.KEY_MARKER, required = false) String keyMarker,
            @RequestParam(value = RestParamDefine.ListUploadsPara.UPLOAD_ID_MARKER, required = false) Long uploadIdMarker,
            @RequestParam(value = RestParamDefine.ListUploadsPara.ENCODING_TYPE, required = false) String encodingType,
            @RequestParam(value = RestParamDefine.ListUploadsPara.MAX_UPLOADS, required = false, defaultValue = "1000") Integer maxUploads,
            ScmSession session) throws S3ServerException {
        try {
            logger.debug(
                    "listUploads. bucketName={}, prefix={}, delimiter={}, keyMarker={}, uploadIdMarker={}",
                    bucketName, prefix, delimiter, keyMarker, uploadIdMarker);

            if (delimiter != null && delimiter.length() == 0) {
                delimiter = null;
            }

            if (prefix != null && prefix.length() == 0) {
                prefix = null;
            }

            if (null != encodingType) {
                if (!encodingType.equals(RestParamDefine.ENCODING_TYPE_URL)) {
                    throw new S3ServerException(S3Error.OBJECT_INVALID_ENCODING_TYPE,
                            "encoding type must be url");
                }
            }

            ListMultipartUploadsResult result = multiPartService.listUploadLists(session,
                    bucketName, prefix, delimiter, keyMarker, uploadIdMarker, maxUploads,
                    encodingType);

            logger.debug("listUploads success. bucketName={}, upload.size={}, commonPrefix.size={}",
                    bucketName, result.getUploadList().size(), result.getCommonPrefixList().size());
            return ResponseEntity.ok().body(result);
        }
        catch (Exception e) {
            logger.info(
                    "listUploads failed. bucketName={}, prefix={}, delimiter={}, keyMarker={}, uploadIdMarker={}",
                    bucketName, prefix, delimiter, keyMarker, uploadIdMarker);
            throw e;
        }
    }

    @RequestMapping(value = "/{bucketname:.+}", params = RestParamDefine.UPLOADID, produces = MediaType.APPLICATION_XML_VALUE)
    public void RejectUploadId(HttpServletRequest httpServletRequest) throws S3ServerException {
        try {
            // 异常请求，携带了uploadId参数，却没有带对象名称，返回错误码“need a key”
            throw new S3ServerException(S3Error.NEED_A_KEY, "need a key");
        }
        catch (Exception e) {
            restUtils.skipInputStream(httpServletRequest);
            throw e;
        }
        finally {
            restUtils.closeInputStream(httpServletRequest);
        }
    }

    private CompleteMultipartUpload getCompleteMap(HttpServletRequest httpServletRequest)
            throws S3ServerException {
        int ONCE_READ_BYTES = 1024;
        CompleteMultipartUpload completeMultipartUpload = null;
        try {
            ServletInputStream inputStream = httpServletRequest.getInputStream();
            StringBuilder stringBuilder = new StringBuilder();
            byte[] b = new byte[ONCE_READ_BYTES];
            int len = inputStream.read(b, 0, ONCE_READ_BYTES);
            while (len > 0) {
                stringBuilder.append(new String(b, 0, len));
                len = inputStream.read(b, 0, ONCE_READ_BYTES);
            }
            String content = stringBuilder.toString();
            if (content.length() > 0) {
                ObjectMapper objectMapper = new XmlMapper();
                completeMultipartUpload = objectMapper.readValue(content,
                        CompleteMultipartUpload.class);
            }
            if (completeMultipartUpload == null) {
                throw new S3ServerException(S3Error.MALFORMED_XML,
                        "completeMultipartUpload is null.");
            }
            if (completeMultipartUpload.getPart() == null) {
                throw new S3ServerException(S3Error.MALFORMED_XML,
                        "completeMultipartUpload is empty, there is no part list.");
            }
            return completeMultipartUpload;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.MALFORMED_XML, "get part list failed", e);
        }
    }

    private int getPartNumber(String partNumberStr) throws S3ServerException {
        int partNumber = restUtils.convertPartNumber(partNumberStr);
        if (partNumber < RestParamDefine.PART_NUMBER_MIN
                || partNumber > RestParamDefine.PART_NUMBER_MAX) {
            throw new S3ServerException(S3Error.PART_INVALID_PARTNUMBER,
                    "invalid partNumber:" + partNumber);
        }
        return partNumber;
    }

    private long getCopyContentLength(Range range, GetObjectResult sourceObject)
            throws S3ServerException {
        checkCopyRange(range, sourceObject.getMeta().getSize());

        long contentLength = sourceObject.getMeta().getSize();
        if (range != null) {
            contentLength = range.getContentLength();
        }
        return contentLength;
    }

    private void checkCopyRange(Range range, long datalength) throws S3ServerException {
        if (null == range) {
            return;
        }

        // range: -end
        if (range.getStart() == -1) {
            throw new S3ServerException(S3Error.PART_COPY_RANGE_INVALID,
                    " range is invalid. start is null, range:-" + range.getEnd());
        }

        // range: start-
        if (range.getEnd() == -1) {
            throw new S3ServerException(S3Error.PART_COPY_RANGE_INVALID,
                    " range is invalid. end is null, range:" + range.getStart() + "-");
        }

        if (range.getStart() >= datalength) {
            throw new S3ServerException(S3Error.PART_COPY_RANGE_NOT_SATISFIABLE,
                    "start > data length. start:" + range.getStart() + ", data length:"
                            + datalength);
        }

        if (range.getEnd() >= datalength) {
            throw new S3ServerException(S3Error.PART_COPY_RANGE_NOT_SATISFIABLE,
                    "end > data length. end:" + range.getEnd() + ", data length:" + datalength);
        }
    }

    void checkMeta(UploadMeta meta) throws S3ServerException {
        if (meta.getKey().length() > RestParamDefine.KEY_LENGTH) {
            throw new S3ServerException(S3Error.OBJECT_KEY_TOO_LONG,
                    "ObjectName is too long. objectName:" + meta.getKey());
        }

        if (meta.getMetaListlength() > RestParamDefine.X_AMZ_META_LENGTH) {
            throw new S3ServerException(S3Error.OBJECT_METADATA_TOO_LARGE,
                    "metadata headers exceed the maximum. xMeta:" + meta.getMetaList());
        }
    }

    private ScmBucket getBucket(ScmUser user, String name) throws S3ServerException {
        try {
            return bucketService.getBucket(user, name);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST, "bucket not exist:" + name,
                        e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED, "", e);
            }
            throw new S3ServerException(S3Error.BUCKET_GET_FAILED,
                    "failed to put object: bucket=" + name, e);
        }
    }

}
