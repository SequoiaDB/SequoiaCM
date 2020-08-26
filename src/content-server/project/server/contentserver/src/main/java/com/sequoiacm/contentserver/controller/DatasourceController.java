package com.sequoiacm.contentserver.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.DatasourceReaderDao;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.service.IDatasourceService;

@RestController
@RequestMapping("/internal/v1")
public class DatasourceController {
    private final IDatasourceService datasourceService;

    @Autowired
    public DatasourceController(IDatasourceService service) {
        datasourceService = service;
    }

    @DeleteMapping("/datasource/{data_id}")
    public void deleteData(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime)
            throws ScmServerException {
        datasourceService.deleteData(wsName, dataId, type, createTime);
    }

    @GetMapping("/datasource/{data_id}")
    public void readData(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            @RequestParam(CommonDefine.RestArg.FILE_READ_FLAG) int readFlag,
            HttpServletResponse response) throws ScmServerException {
        response.setHeader("Content-Disposition", "attachment; filename=" + dataId);

        ServletOutputStream os = RestUtils.getOutputStream(response);
        DatasourceReaderDao dao = datasourceService.readData(wsName, dataId, type, createTime,
                readFlag, os);
        try {
            response.setHeader(CommonDefine.RestArg.DATA_LENGTH, dao.getSize() + "");
            dao.read(os);
        }
        finally {
            dao.close();
        }

        RestUtils.flush(os);
    }

    // TODO:数据不存在：404
    @RequestMapping(value = "/datasource/{data_id}", method = RequestMethod.HEAD)
    public void headDataInfo(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            HttpServletResponse response) throws ScmServerException {
        BSONObject dataInfo = datasourceService.getDataInfo(wsName, dataId, type, createTime);
        response.setHeader(CommonDefine.RestArg.DATASOURCE_DATA_HEADER, dataInfo.toString());
    }

    @PostMapping(value = "/datasource/{data_id}")
    public void createData(@PathVariable("data_id") String dataId,
            @RequestParam(CommonDefine.RestArg.WORKSPACE_NAME) String wsName,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TYPE) int type,
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_CREATE_TIME) long createTime,
            HttpServletRequest request) throws ScmServerException, IOException {
        InputStream is = request.getInputStream();
        try {
            datasourceService.createData(wsName, dataId, type, createTime, is);
        }
        finally {
            ScmSystemUtils.consumeAndCloseResource(is);
        }
    }

    @DeleteMapping(value = "/datasource/tables")
    public void deleteDataTable(
            @RequestParam(CommonDefine.RestArg.DATASOURCE_DATA_TABLE_NAMES) List<String> tableNames)
                    throws ScmServerException {
        datasourceService.deleteDataTables(tableNames);
    }
}
