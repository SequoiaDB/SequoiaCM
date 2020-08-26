package com.sequoiacm.content.client.remote;

import org.bson.BSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmServerException;

import feign.Response;

@RequestMapping("/internal/v1")
public interface ContentserverFeign {
    @RequestMapping(value = "/files?action=list", method = RequestMethod.GET)
    public Response fileList(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER) BSONObject condition,
            @RequestParam(CommonDefine.RestArg.FILE_LIST_SCOPE) int scope,
            @RequestParam(value = CommonDefine.RestArg.FILE_ORDERBY) BSONObject orderby,
            @RequestParam(value = CommonDefine.RestArg.FILE_SKIP) long skip,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIMIT) long limit,
            @RequestParam(value = CommonDefine.RestArg.FILE_SELECTOR) BSONObject selector)
            throws ScmServerException;

    @PostMapping(value = "/files/{file_id}?action=updateExternalData")
    public boolean updateFileExternalData(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.FILE_MAJOR_VERSION) int majorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(CommonDefine.RestArg.FILE_EXTERNAL_DATA) BSONObject externalData)
            throws ScmServerException;

    @PostMapping(value = "/files?action=updateExternalData")
    public void updateFileExternalData(
            @RequestParam(CommonDefine.RestArg.FILE_EXTERNAL_DATA) BSONObject externalData,
            @RequestParam(CommonDefine.RestArg.FILE_FILTER) BSONObject matcher,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName)
            throws ScmServerException;

    @RequestMapping(value = "/files?action=count", method = RequestMethod.GET)
    public long countFile(@RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(value = CommonDefine.RestArg.FILE_LIST_SCOPE) int scope,
            @RequestParam(value = CommonDefine.RestArg.FILE_FILTER) BSONObject condition)
            throws ScmServerException;

    @GetMapping("/files/{file_id}?action=download")
    public Response downloadFile(
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspace_name,
            @PathVariable("file_id") String fileId,
            @RequestParam(value = CommonDefine.RestArg.FILE_MAJOR_VERSION) int majorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_FLAG) int readFlag,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_OFFSET) long offset,
            @RequestParam(value = CommonDefine.RestArg.FILE_READ_LENGTH) int length)
            throws ScmServerException;

    @RequestMapping(value = "/files/{file_id}?action=get_info", method = RequestMethod.GET)
    public BSONObject getFileInfo(@PathVariable("file_id") String fileId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String workspaceName,
            @RequestParam(name = CommonDefine.RestArg.FILE_MAJOR_VERSION) int majorVersion,
            @RequestParam(name = CommonDefine.RestArg.FILE_MINOR_VERSION) int minorVersion)
            throws ScmServerException;
}
