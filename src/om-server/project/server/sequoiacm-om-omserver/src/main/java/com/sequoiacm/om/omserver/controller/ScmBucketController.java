package com.sequoiacm.om.omserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.om.omserver.common.CommonUtil;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmBucketDetail;
import com.sequoiacm.om.omserver.module.OmFileBasic;
import com.sequoiacm.om.omserver.module.OmFileInfo;
import com.sequoiacm.om.omserver.service.ScmBucketService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScmBucketController {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ScmBucketService bucketService;

    @GetMapping(value = "/buckets", params = "action=get_related_buckets")
    public List<String> listBucket(ScmOmSession session)
            throws ScmInternalException, ScmOmServerException {
        return bucketService.getUserAccessibleBuckets(session);
    }

    @RequestMapping(value = "/buckets/{bucket_name}", method = RequestMethod.HEAD)
    public ResponseEntity<Object> getBucketDetail(@PathVariable("bucket_name") String bucketName,
            ScmOmSession session)
            throws JsonProcessingException, ScmInternalException, ScmOmServerException {
        OmBucketDetail bucketDetail = bucketService.getBucketDetail(session, bucketName);
        return ResponseEntity.ok()
                .header(RestParamDefine.BUCKET, mapper.writeValueAsString(bucketDetail)).build();
    }

    @GetMapping("/buckets/{name}/files")
    public List<OmFileBasic> listFiles(ScmOmSession session,
            @PathVariable("name") String bucketName,
            @RequestParam(value = RestParamDefine.SKIP, required = false, defaultValue = "0") long skip,
            @RequestParam(value = RestParamDefine.LIMIT, required = false, defaultValue = "1000") int limit,
            @RequestParam(value = RestParamDefine.FILTER, required = false, defaultValue = "{}") BSONObject filter,
            @RequestParam(value = RestParamDefine.ORDERBY, required = false) BSONObject orderBy,
            HttpServletResponse response) throws ScmInternalException, ScmOmServerException {
        long fileCount = bucketService.countFile(session, bucketName, filter);
        response.setHeader(RestParamDefine.X_RECORD_COUNT, String.valueOf(fileCount));
        if (fileCount <= 0) {
            return Collections.emptyList();
        }
        return bucketService.listFiles(session, bucketName, filter, orderBy, skip, limit);
    }

    @PostMapping(value = "/buckets/{bucket_name}/files")
    public void createFile(ScmOmSession session, @PathVariable("bucket_name") String bucketName,
            @RequestParam(value = RestParamDefine.SITE_NAME) String siteName,
            @RequestParam(value = RestParamDefine.FILE_UPLOAD_CONFIG, required = false, defaultValue = "{}") BSONObject uploadConf,
            @RequestHeader(value = RestParamDefine.FILE_DESCRIPTION, required = false) String desc,
            HttpServletRequest request)
            throws ScmInternalException, ScmOmServerException, IOException {
        InputStream is = request.getInputStream();
        try {
            OmFileInfo fileInfo = mapper.readValue(CommonUtil.urlDecode(desc), OmFileInfo.class);
            bucketService.createFile(session, bucketName, siteName, fileInfo, uploadConf, is);
        }
        finally {
            CommonUtil.consumeAndCloseResource(is);
        }
    }
}
